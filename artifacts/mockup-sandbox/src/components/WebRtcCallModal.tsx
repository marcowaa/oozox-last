import React, { useEffect, useRef, useState } from 'react';
import { Mic, MicOff, Phone, PhoneOff, Video, VideoOff, AlertTriangle, ShieldCheck } from 'lucide-react';
import {
  apiAuthorizeCall,
  apiFetchTurnCredentials,
  getSignalingWsUrl,
} from '../lib/webAuthClient';

export interface ActiveWebCallSession {
  callId: string;
  peerId: string;
  peerName: string;
  callType: 'AUDIO' | 'VIDEO';
  isIncoming: boolean;
  offerSdp?: string;
}

export const WebRtcCallModal: React.FC<{
  token: string;
  currentUserId: string;
  callSession: ActiveWebCallSession | null;
  wsRef: React.MutableRefObject<WebSocket | null>;
  incomingSignal: Record<string, unknown> | null;
  onClose: () => void;
  onBudgetBlocked: (msg: string) => void;
}> = ({
  token,
  currentUserId,
  callSession,
  wsRef,
  incomingSignal,
  onClose,
  onBudgetBlocked,
}) => {
  const [status, setStatus] = useState<'PREPARING' | 'RINGING' | 'CONNECTED' | 'ENDED'>('PREPARING');
  const [statusText, setStatusText] = useState('جاري التحقق من الجلسة وتفويض المكالمة...');
  const [muted, setMuted] = useState(false);
  const [cameraOff, setCameraOff] = useState(false);
  const [elapsedSec, setElapsedSec] = useState(0);

  const pcRef = useRef<RTCPeerConnection | null>(null);
  const localStreamRef = useRef<MediaStream | null>(null);
  const pendingIceRef = useRef<RTCIceCandidateInit[]>([]);
  const remoteDescSetRef = useRef(false);
  const localVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteVideoRef = useRef<HTMLVideoElement | null>(null);
  const remoteAudioRef = useRef<HTMLAudioElement | null>(null);

  const cleanupRtc = () => {
    try {
      localStreamRef.current?.getTracks().forEach((t) => t.stop());
      pcRef.current?.close();
    } catch {
      // ignore cleanup errors
    }
    localStreamRef.current = null;
    pcRef.current = null;
    pendingIceRef.current = [];
    remoteDescSetRef.current = false;
  };

  const sendSignal = (payload: Record<string, unknown>) => {
    const ws = wsRef.current;
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify(payload));
    }
  };

  useEffect(() => {
    if (status !== 'CONNECTED') {
      setElapsedSec(0);
      return undefined;
    }
    const timer = setInterval(() => setElapsedSec((s) => s + 1), 1000);
    return () => clearInterval(timer);
  }, [status]);

  useEffect(() => {
    if (!callSession) return undefined;
    let cancelled = false;

    const startCallFlow = async () => {
      try {
        let authData: { callId: string; exp: number; callAuthSignature: string } | null = null;
        if (!callSession.isIncoming) {
          setStatus('PREPARING');
          setStatusText('التحقق من الميزانية الشهرية للمكالمات...');
          const authRes = await apiAuthorizeCall(token, callSession.peerId, callSession.callType);
          if (!authRes.ok || !authRes.data?.allowed) {
            onBudgetBlocked(authRes.message || 'تم إيقاف المكالمات الجديدة لبلوغ الحد الشهري (100%).');
            onClose();
            return;
          }
          authData = {
            callId: authRes.data.callId || callSession.callId,
            exp: authRes.data.exp,
            callAuthSignature: authRes.data.callAuthSignature,
          };
        }

        const iceServers = await apiFetchTurnCredentials(token, 3600);
        if (cancelled) return;

        const media = await navigator.mediaDevices.getUserMedia({
          audio: true,
          video: callSession.callType === 'VIDEO',
        });
        if (cancelled) {
          media.getTracks().forEach((t) => t.stop());
          return;
        }
        localStreamRef.current = media;
        if (localVideoRef.current && callSession.callType === 'VIDEO') {
          localVideoRef.current.srcObject = media;
        }

        const pc = new RTCPeerConnection({ iceServers });
        pcRef.current = pc;

        media.getTracks().forEach((track) => pc.addTrack(track, media));

        pc.ontrack = (ev) => {
          const stream = ev.streams?.[0];
          if (!stream) return;
          if (remoteVideoRef.current && callSession.callType === 'VIDEO') {
            remoteVideoRef.current.srcObject = stream;
          }
          if (remoteAudioRef.current) {
            remoteAudioRef.current.srcObject = stream;
          }
        };

        pc.onicecandidate = (ev) => {
          if (ev.candidate) {
            sendSignal({
              type: 'ICE_CANDIDATE',
              callId: authData?.callId || callSession.callId,
              target: callSession.peerId,
              senderId: currentUserId,
              callType: callSession.callType,
              candidate: ev.candidate.candidate,
              sdpMid: ev.candidate.sdpMid,
              sdpMLineIndex: ev.candidate.sdpMLineIndex,
            });
          }
        };

        pc.onconnectionstatechange = () => {
          if (pc.connectionState === 'connected') {
            setStatus('CONNECTED');
            setStatusText('اتصال مشفر مباشر (DTLS-SRTP)');
          } else if (pc.connectionState === 'failed' || pc.connectionState === 'closed') {
            setStatus('ENDED');
          }
        };

        if (!callSession.isIncoming && authData) {
          const offer = await pc.createOffer();
          await pc.setLocalDescription(offer);
          setStatus('RINGING');
          setStatusText('جاري الاتصال والرنين...');
          sendSignal({
            type: 'CALL_OFFER',
            callId: authData.callId,
            target: callSession.peerId,
            senderId: currentUserId,
            callType: callSession.callType,
            sdp: offer.sdp || '',
            callAuthExp: authData.exp,
            callAuthSignature: authData.callAuthSignature,
          });
        } else if (callSession.isIncoming) {
          setStatus('RINGING');
          setStatusText('مكالمة واردة — اضغط قبول للرد');
        }
      } catch (err) {
        if (!cancelled) {
          onBudgetBlocked(err instanceof Error ? err.message : 'تعذر الوصول إلى الميكروفون أو الكاميرا.');
          cleanupRtc();
          onClose();
        }
      }
    };

    void startCallFlow();
    return () => {
      cancelled = true;
      cleanupRtc();
    };
  }, [callSession?.callId]);

  // Process incoming WebSocket signaling events during active modal
  useEffect(() => {
    if (!incomingSignal || !callSession || !pcRef.current) return;
    const type = String(incomingSignal.type || '');
    const sigCallId = String(incomingSignal.callId || '');
    if (sigCallId && sigCallId !== callSession.callId) return;

    const pc = pcRef.current;
    if (type === 'CALL_ACCEPT' && typeof incomingSignal.sdp === 'string') {
      void pc
        .setRemoteDescription(new RTCSessionDescription({ type: 'answer', sdp: incomingSignal.sdp }))
        .then(async () => {
          remoteDescSetRef.current = true;
          for (const ice of pendingIceRef.current) {
            await pc.addIceCandidate(new RTCIceCandidate(ice));
          }
          pendingIceRef.current = [];
          setStatus('CONNECTED');
          setStatusText('اتصال مشفر مباشر (DTLS-SRTP)');
        });
    } else if (type === 'ICE_CANDIDATE' && typeof incomingSignal.candidate === 'string') {
      const candidate: RTCIceCandidateInit = {
        candidate: incomingSignal.candidate,
        sdpMid: (incomingSignal.sdpMid as string) || '0',
        sdpMLineIndex: Number(incomingSignal.sdpMLineIndex || 0),
      };
      if (remoteDescSetRef.current) {
        void pc.addIceCandidate(new RTCIceCandidate(candidate));
      } else {
        pendingIceRef.current.push(candidate);
      }
    } else if (type === 'CALL_REJECT' || type === 'CALL_END') {
      cleanupRtc();
      onClose();
    } else if (type === 'CALL_BUDGET_BLOCKED') {
      onBudgetBlocked(String(incomingSignal.message || 'تم منع بدء المكالمة لبلوغ حد الميزانية الشهرية.'));
      cleanupRtc();
      onClose();
    }
  }, [incomingSignal]);

  if (!callSession) return null;

  const handleAcceptIncoming = async () => {
    const pc = pcRef.current;
    if (!pc || !callSession.offerSdp) return;
    await pc.setRemoteDescription(new RTCSessionDescription({ type: 'offer', sdp: callSession.offerSdp }));
    remoteDescSetRef.current = true;
    for (const ice of pendingIceRef.current) {
      await pc.addIceCandidate(new RTCIceCandidate(ice));
    }
    pendingIceRef.current = [];
    const answer = await pc.createAnswer();
    await pc.setLocalDescription(answer);
    sendSignal({
      type: 'CALL_ACCEPT',
      callId: callSession.callId,
      target: callSession.peerId,
      senderId: currentUserId,
      callType: callSession.callType,
      sdp: answer.sdp || '',
    });
    setStatus('CONNECTED');
    setStatusText('اتصال مشفر مباشر (DTLS-SRTP)');
  };

  const handleHangup = () => {
    sendSignal({
      type: status === 'RINGING' && callSession.isIncoming ? 'CALL_REJECT' : 'CALL_END',
      callId: callSession.callId,
      target: callSession.peerId,
      senderId: currentUserId,
      callType: callSession.callType,
      reason: 'HANGUP',
    });
    cleanupRtc();
    onClose();
  };

  const toggleMute = () => {
    const next = !muted;
    localStreamRef.current?.getAudioTracks().forEach((t) => {
      t.enabled = !next;
    });
    setMuted(next);
  };

  const toggleVideo = () => {
    const next = !cameraOff;
    localStreamRef.current?.getVideoTracks().forEach((t) => {
      t.enabled = !next;
    });
    setCameraOff(next);
  };

  const formatTimer = (sec: number) => {
    const m = Math.floor(sec / 60);
    const s = sec % 60;
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/90 backdrop-blur-md flex items-center justify-center p-4" dir="rtl">
      <div className="w-full max-w-lg bg-slate-900 border border-amber-500/30 rounded-3xl p-6 shadow-2xl space-y-5 text-white">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2 text-emerald-400 text-xs font-bold">
            <ShieldCheck className="w-4 h-4" />
            <span>مكالمة {callSession.callType === 'VIDEO' ? 'فيديو' : 'صوتية'} P2P E2EE</span>
          </div>
          {status === 'CONNECTED' && (
            <span className="font-mono text-xs font-black px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-300">
              {formatTimer(elapsedSec)}
            </span>
          )}
        </div>

        <audio ref={remoteAudioRef} autoPlay />

        {callSession.callType === 'VIDEO' ? (
          <div className="relative rounded-2xl overflow-hidden bg-slate-950 border border-slate-800 aspect-video">
            <video ref={remoteVideoRef} autoPlay playsInline className="w-full h-full object-cover" />
            <video
              ref={localVideoRef}
              autoPlay
              muted
              playsInline
              className="w-28 h-20 object-cover rounded-xl border border-amber-500/40 absolute bottom-3 left-3 bg-slate-900"
            />
          </div>
        ) : (
          <div className="py-8 text-center space-y-2">
            <div className="w-20 h-20 rounded-3xl bg-amber-500/15 border border-amber-500/40 text-amber-400 font-black text-3xl flex items-center justify-center mx-auto">
              {callSession.peerName.charAt(0)}
            </div>
          </div>
        )}

        <div className="text-center space-y-1">
          <h3 className="text-xl font-black">{callSession.peerName}</h3>
          <p className="text-xs text-slate-400">{statusText}</p>
        </div>

        <div className="flex items-center justify-center gap-4 pt-2">
          <button
            type="button"
            onClick={toggleMute}
            className={`w-12 h-12 rounded-2xl flex items-center justify-center border transition-all ${
              muted ? 'bg-rose-500/20 border-rose-500 text-rose-300' : 'bg-slate-800 border-slate-700 text-white'
            }`}
            title={muted ? 'إلغاء كتم الصوت' : 'كتم الصوت'}
          >
            {muted ? <MicOff className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
          </button>

          {callSession.callType === 'VIDEO' && (
            <button
              type="button"
              onClick={toggleVideo}
              className={`w-12 h-12 rounded-2xl flex items-center justify-center border transition-all ${
                cameraOff ? 'bg-rose-500/20 border-rose-500 text-rose-300' : 'bg-slate-800 border-slate-700 text-white'
              }`}
              title={cameraOff ? 'تشغيل الكاميرا' : 'إيقاف الكاميرا'}
            >
              {cameraOff ? <VideoOff className="w-5 h-5" /> : <Video className="w-5 h-5" />}
            </button>
          )}

          {callSession.isIncoming && status === 'RINGING' && (
            <button
              type="button"
              onClick={handleAcceptIncoming}
              className="w-14 h-14 rounded-2xl bg-emerald-500 hover:bg-emerald-400 text-slate-950 flex items-center justify-center shadow-lg"
              title="رد وقبول المكالمة"
            >
              <Phone className="w-6 h-6" />
            </button>
          )}

          <button
            type="button"
            onClick={handleHangup}
            className="w-14 h-14 rounded-2xl bg-rose-600 hover:bg-rose-500 text-white flex items-center justify-center shadow-lg"
            title="إنهاء المكالمة"
          >
            <PhoneOff className="w-6 h-6" />
          </button>
        </div>
      </div>
    </div>
  );
};
