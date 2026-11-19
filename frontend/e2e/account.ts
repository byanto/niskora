/**
 * The account the journeys log in with. It is invented, and the CI job creates it on an empty
 * stack with the first-user command. Locally, create it the same way, or point these variables at
 * an account of your own.
 */
export const account = {
  loginName: process.env.E2E_LOGIN_NAME ?? "journey",
  fullName: process.env.E2E_FULL_NAME ?? "Journey Tester",
  password: process.env.E2E_PASSWORD ?? "journey password",
};
