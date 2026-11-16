# Agent: the read-only agent

What the agent answers, the tools it answers with, the model behind it, and how buyer data is kept away from it. Rule ids start with `KRN`.

Read [README.md](README.md) first. Terms are defined in [`GLOSSARY.md`](../../GLOSSARY.md) and are not redefined here.

## 1. What the agent is

- **AGT-1** The [agent](../../GLOSSARY.md#agent) answers a [user](../../GLOSSARY.md#people)'s questions in plain language from niskora's own data: the catalogue, demand, stock, restock advice, purchase orders and location analysis. It gives no general business advice and uses no outside knowledge.
- **AGT-2** It is strictly read-only. It creates no Draft purchase order, no campaign day and no place alias. Where something can be done, it links to the screen where the user does it.
- **AGT-3** It answers only when asked. It sends no reminders and no digests.
- **AGT-4** It has its own page in the app, and the answer streams in.
- **AGT-5** It answers in the language of the question, Bahasa Indonesia or English.
- **AGT-6** It may see everything a user sees except buyer fields. That includes the [Weighted Average Cost](../../GLOSSARY.md#cost), goods prices, suppliers and supply sources.
- **AGT-7** Planned usage is 3 to 5 users and 20 to 50 questions a day. One question is about 4 requests to the model.

_Sources: [AI agent design and provider](https://github.com/byanto/niskora/issues/11)._

## 2. Conversations

- **AGT-8** Each user has saved [conversations](../../GLOSSARY.md#agent). A conversation is private to its user, even though v1 has no roles.
- **AGT-9** A conversation is kept until its user deletes it.
- **AGT-10** Nothing is remembered from one conversation to the next.
- **AGT-11** Each answer records the model that produced it, the tools it ran and the token counts.

_Sources: [AI agent design and provider](https://github.com/byanto/niskora/issues/11)._

## 3. Tools

- **AGT-12** The agent works only through fixed, typed, read-only [tools](../../GLOSSARY.md#agent). Each tool calls a module's public query interface, the same one the screens use. The agent never writes SQL ([ADR 0005](../adr/0005-the-agent-answers-through-fixed-tools-and-never-writes-sql.md)).
- **AGT-13** The tools are:

  | No. | Module | Tool |
  | --- | --- | --- |
  | 1 | `catalogue` | Find Merchant SKUs and SPUs |
  | 2 | `catalogue` | Merchant SKU details: components, supply sources, Weighted Average Cost |
  | 3 | `orders` | Demand for a period, grouped by Merchant SKU, SPU, company, store, marketplace, week or month, against the preceding period |
  | 4 | `stock` | Stock per Single SKU and its stockout days |
  | 5 | `restock` | Restock advice by supplier or SPU, with the working |
  | 6 | `restock` | Why a Merchant SKU gets no advice |
  | 7 | `purchasing` | Purchase orders by status, by supplier or Overdue, with their lines and on order |
  | 8 | `purchasing` | Unresolved shortfalls and unmatched stock-ins |
  | 9 | `purchasing` | Suppliers: lead time, order cycle, supply sources |
  | 10 | `locations` | Location rankings: places for an SPU, SPUs for a place |
  | 11 | `imports` | Data freshness: the last import per data set, and whether it is Stale |

- **AGT-14** Every tool takes filters and returns at most 50 rows plus the total count.
- **AGT-15** Every figure in an answer comes from a tool result. The tools do the sums and the rankings. The model explains and compares.
- **AGT-16** Tools return no free text taken from order exports, such as buyer notes and remarks.
- **AGT-17** No module knows that `agent` exists, and `agent` holds no business data of its own. It keeps only conversations.
- **AGT-18** Each tool answers in under 1 second on ten years of synthetic data (ENG-27).

_Sources: [AI agent design and provider](https://github.com/byanto/niskora/issues/11), [Reporting and read model approach](https://github.com/byanto/niskora/issues/15)._

## 4. Answers

- **AGT-19** Every answer lists the tools it ran with their filters, and links to the matching screen.
- **AGT-20** Every answer states the date its data runs up to, and warns when a data set it used is [Stale](../../GLOSSARY.md#imports).
- **AGT-21** A question that no tool covers gets "I can't answer that yet".

_Sources: [AI agent design and provider](https://github.com/byanto/niskora/issues/11)._

## 5. Provider, model and cost

- **AGT-22** The provider is the Gemini API on its free tier, with a key from Google AI Studio and no billing enabled. v1 spends nothing on AI.
- **AGT-23** The agent is built on Spring AI's `ChatClient` with the Google GenAI starter. Provider and model are configuration, so a switch is a configuration change. The API key is a secret on the machine ([platform.md](platform.md), PLT-12) and is never in the repository or in an issue.
- **AGT-24** Under the free tier's terms Google may use the questions and the tool results to improve its products, and human reviewers may read them. The owner accepts this for sales figures, costs, goods prices and supplier names. Buyer data never goes out. Moving to the paid tier later is a billing change only.
- **AGT-25** The main model is `gemini-3.5-flash-lite`. Its free tier limits, as read from the owner's AI Studio page, are 15 requests a minute, 250,000 tokens a minute and 500 requests a day. That is about 125 questions a day against a plan of 20 to 50.
- **AGT-26** An optional fallback model is `gemini-3.1-flash-lite`, which has its own limits of the same size. The fallback can be left empty.
- **AGT-27** When Gemini answers that a limit is reached (HTTP 429) on the main model, the agent tries that same request once more on the fallback model: the same provider, the same API key and the same data terms.
- **AGT-28** There is no automatic fallback to a second provider.
- **AGT-29** When the fallback is limited as well, or none is configured, the agent page says that the limit is reached and when it resets. A daily limit resets at midnight Pacific time, and the page shows that moment in Jakarta time as well. For a per-minute limit the page says to try again in a minute. The rest of niskora is unaffected.
- **AGT-30** The Flash models allow 20 requests a day on the free tier and are not used. If Flash-Lite answers poorly on the question set, the choice between the paid tier and another provider is a new decision at that point.

_Sources: [AI agent design and provider](https://github.com/byanto/niskora/issues/11), [Create a Gemini API key and read the free tier limits](https://github.com/byanto/niskora/issues/22), [Final review of the v1 spec](https://github.com/byanto/niskora/issues/36)._

## 6. Keeping buyer data out

- **AGT-31** The agent's tools never return a [buyer](../../GLOSSARY.md#orders) field: no name, phone, address, post code, Shopee username or Customer Code. This is enforced by structure and proven by three tests.
- **AGT-32** Structure: `agent` depends only on other modules' public query interfaces, and none of those carries buyer fields. Modulith verification fails the build if `agent` reaches the buyer part of `orders` (ENG-21).
- **AGT-33** An architecture test fails if the return type of any tool contains a buyer type.
- **AGT-34** A use case test seeds packages with distinctive fake buyer names, phones and addresses, runs every tool, and asserts that none of those values appears in anything sent to the model.

_Sources: [AI agent design and provider](https://github.com/byanto/niskora/issues/11)._

## 7. Testing

- **AGT-35** On every push, a scripted stand-in for the model covers: the tool loop, saved conversations, the limit message, the personal-data rule, the main model limited and the fallback answering, and both models limited.
- **AGT-36** A set of about 30 real questions with known correct figures is run by hand against the real model, on the main model and on the fallback, before a release and after any change to the prompt, the tools or the model. It is not a required check.
- **AGT-37** Running the question set is also the moment to read the rate limit page again.

_Sources: [AI agent design and provider](https://github.com/byanto/niskora/issues/11), [Create a Gemini API key and read the free tier limits](https://github.com/byanto/niskora/issues/22)._
