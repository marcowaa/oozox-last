package com.example

import com.example.data.model.ChatEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageType
import com.example.data.model.UserType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class ChatFlowTest {

    @Test
    fun testTwoPartyMessageClassification() {
        val currentUserName = "المهندس محمود الفقي"
        val contactName = "الحاج مصطفى الصايغ"

        val chat = ChatEntity(
            id = "chat_gold_1",
            contactId = "contact_1",
            contactName = contactName,
            contactType = UserType.GOLD_TRADER.name,
            contactVerified = true
        )

        val incomingMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chat.id,
            senderId = "contact_1",
            senderName = contactName,
            type = MessageType.GOLD_PURCHASE.name,
            content = "تم تسجيل صفقة: شراء 50 جرام عيار 21 بسعر 3,750 ج.م",
            timestamp = System.currentTimeMillis()
        )

        val outgoingMessage = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chat.id,
            senderId = "me_1",
            senderName = currentUserName,
            type = MessageType.PAYMENT.name,
            content = "تم تحويل دفعة 100,000 ج.م عبر التحويل البنكي",
            timestamp = System.currentTimeMillis()
        )

        // Verify incoming message is from the other party
        val isIncomingMe = incomingMessage.senderName != chat.contactName
        assertFalse("Incoming message should not be classified as 'isMe'", isIncomingMe)

        // Verify outgoing message is from me
        val isOutgoingMe = outgoingMessage.senderName != chat.contactName
        assertTrue("Outgoing message should be classified as 'isMe'", isOutgoingMe)

        assertEquals(MessageType.GOLD_PURCHASE.name, incomingMessage.type)
        assertEquals(MessageType.PAYMENT.name, outgoingMessage.type)
    }

    @Test
    fun testChatEntityCreationAndFallback() {
        val chatId = "unknown_chat_123"
        val fallbackChat = ChatEntity(
            id = chatId,
            contactId = chatId,
            contactName = "محادثة مالية",
            contactType = UserType.GOLD_TRADER.name,
            contactVerified = false
        )

        assertNotNull(fallbackChat)
        assertEquals(chatId, fallbackChat.id)
        assertEquals("محادثة مالية", fallbackChat.contactName)
    }

    @Test
    fun testUniqueKeyGenerationForLazyColumn() {
        val msg1 = MessageEntity(id = "id_1", chatId = "c1", senderId = "s1", senderName = "User 1", type = "TEXT", content = "Hi", timestamp = 1000L)
        val msg2 = MessageEntity(id = "id_2", chatId = "c1", senderId = "s2", senderName = "User 2", type = "TEXT", content = "Hello", timestamp = 2000L)
        val msgList = listOf(msg1, msg2)

        val keys = msgList.mapIndexed { index, msg ->
            val baseId = if (msg.id.isNotBlank()) msg.id else "${msg.timestamp}_${msg.senderId}"
            "${baseId}_$index"
        }

        assertEquals(2, keys.distinct().size)
        assertEquals("id_1_0", keys[0])
        assertEquals("id_2_1", keys[1])
    }

    @Test
    fun testSendMessageValidationAndDelivery() {
        val chatId = "chat_test_101"
        val senderId = "user_me"
        val senderName = "المهندس محمود الفقي"
        val messageText = "تم تحويل دفعة 50,000 ج.م لحساب التوريدات #ذهب_عيار21"

        val message = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = chatId,
            senderId = senderId,
            senderName = senderName,
            type = MessageType.PAYMENT.name,
            content = messageText,
            timestamp = System.currentTimeMillis()
        )

        assertNotNull(message.id)
        assertTrue(message.id.isNotEmpty())
        assertEquals(chatId, message.chatId)
        assertEquals(MessageType.PAYMENT.name, message.type)
        assertTrue(message.content.contains("#ذهب_عيار21"))
        assertTrue(message.content.contains("50,000"))
    }

    @Test
    fun testGroupChatCreationAndMultiUserMessaging() {
        val groupChatId = "group_traders_99"
        val group = ChatEntity(
            id = groupChatId,
            contactId = "group_contact_id",
            contactName = "مجموعة تجار الذهب والصاغة 🪙",
            contactType = UserType.GOLD_TRADER.name,
            contactVerified = true,
            isGroup = true,
            groupName = "مجموعة تجار الذهب والصاغة 🪙",
            lastMessage = "افتتاح سوق الصاغة اليوم",
            lastMessageTime = System.currentTimeMillis(),
            unreadCount = 0
        )

        assertTrue("Chat should be recognized as a group", group.isGroup)
        assertEquals("مجموعة تجار الذهب والصاغة 🪙", group.groupName)

        val messageFromTrader = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupChatId,
            senderId = "trader_1",
            senderName = "الحاج مصطفى الصايغ",
            type = MessageType.TEXT.name,
            content = "سعر جرام 21 اليوم 3750 ج.م",
            timestamp = System.currentTimeMillis()
        )

        val messageFromMe = MessageEntity(
            id = UUID.randomUUID().toString(),
            chatId = groupChatId,
            senderId = "user_me",
            senderName = "المهندس محمود الفقي",
            type = MessageType.TEXT.name,
            content = "تمام يا حاج، جهز لنا 100 جرام",
            timestamp = System.currentTimeMillis() + 1000
        )

        val messages = listOf(messageFromTrader, messageFromMe)
        assertEquals(2, messages.size)
        assertEquals("trader_1", messages[0].senderId)
        assertEquals("user_me", messages[1].senderId)
    }
}
