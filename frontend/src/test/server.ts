import { setupServer } from "msw/node";

/**
 * The fake backend for tests (ENG-38). It starts with no handlers, so each test states the
 * answers it relies on with `server.use(...)`, and any request it did not expect fails the test.
 */
export const server = setupServer();
