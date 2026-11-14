# The agent answers through fixed tools and never writes its own SQL

The agent gets a fixed set of typed, read-only tools, each calling one module's public query interface. It cannot write SQL, and the `agent` module has no database access to other modules' schemas. We chose this for two reasons. First, the rule that buyer personal data never reaches the model holds by construction: a tool can only return what a module chooses to expose, and no module's public interface carries buyer fields or free text from order exports. Second, every figure the agent states is the same figure the screens show, because `orders` alone computes demand and `restock` alone computes advice; the model picks tools and words the answer but never totals rows itself. The second reason matters all the more because v1 runs on a small, free model, which is reliable at choosing among a dozen tools and unreliable at writing correct queries.

## Considered options

- **Text-to-SQL over a read-only database role:** answers unusual questions nobody planned a tool for. Rejected because the model would compute demand, stock and advice its own way and disagree with the screens, and because keeping buyer data out would rest on database grants and views instead of on module boundaries.
- **Text-to-SQL over curated reporting views:** narrows the risk but builds a second, parallel definition of every figure that has to be kept in step with the modules.

## Consequences

- A question no tool covers gets "I can't answer that yet". Saved conversations record such questions, and the answer is a new tool.
- Each new screen or figure that the agent should know about needs its own tool.
- Tools return at most 50 rows and a total count, so the model never receives a whole table.
- Three checks guard the personal-data rule: Modulith verification of the `agent` module's dependencies, an architecture test on tool return types, and a use case test that seeds distinctive buyer values and asserts none reaches the model.
