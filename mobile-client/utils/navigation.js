import { createNavigationContainerRef, CommonActions } from '@react-navigation/native';

/**
 * Global Navigation Reference allowing navigation dispatch from non-component modules (e.g. Axios Interceptors)
 */
export const navigationRef = createNavigationContainerRef();

/**
 * Navigate to a specific route safely
 * @param {string} name - Route name
 * @param {object} params - Route parameters
 */
export function navigate(name, params) {
  if (navigationRef.isReady()) {
    navigationRef.navigate(name, params);
  } else {
    console.warn(`[NAVIGATION-REF] Navigation not ready to navigate to: ${name}`);
  }
}

/**
 * Reset navigation state and redirect user to a designated screen (e.g. /onboarding on 401)
 * @param {string} routeName - Target route name (defaults to 'Onboarding')
 */
export function forceRedirectToOnboarding(routeName = 'Onboarding') {
  if (navigationRef.isReady()) {
    navigationRef.dispatch(
      CommonActions.reset({
        index: 0,
        routes: [{ name: routeName }],
      })
    );
  } else {
    console.warn('[NAVIGATION-REF] Navigation container not ready for reset');
  }
}

export default {
  navigationRef,
  navigate,
  forceRedirectToOnboarding,
};
