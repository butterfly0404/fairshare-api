# FairShare API

> Settles a group's shared expenses in the **minimum number of payments**.
> Five people with tangled debts settle in 4 transactions instead of 10+.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![Tests](https://img.shields.io/badge/tests-9%20passing-success)

---

## The problem

When a group shares expenses — roommates, a trip, a team lunch rota — tracking who
owes whom gets messy fast. After 30 expenses across 6 people you have dozens of
small pairwise debts: A owes B ₹120, B owes C ₹120, C owes A ₹80.

Settling those naively means a payment for every single debt. Most of them cancel
out. The question is: **what is the smallest set of payments that settles everyone?**

## The solution

FairShare nets each person down to a single balance, then runs a greedy
two-heap matching algorithm that produces a settlement plan with at most
**n−1 transactions** for n people.

### Example

Five people, tangled debts:

```json
{ "Alice": 1200, "Bob": -450, "Carol": 300, "Dave": -800, "Eve": -250 }
```

Settled in 4 payments:

| From  | To    | Amount |
|-------|-------|--------|
| Dave  | Alice | 800    |
| Bob   | Alice | 400    |
| Eve   | Carol | 250    |
| Bob   | Carol | 50     |

## How it works

Once balances are netted, the original who-paid-for-whom graph is irrelevant —
only each person's net position matters.

Two max-heaps are maintained: creditors and debtors. Each iteration takes the
largest of each and settles the smaller amount between them. That payment zeroes
out at least one of the pair, so every iteration permanently removes at least one
participant — giving the n−1 bound and **O(n log n)** overall.

> **On optimality:** finding the true minimum in every case means detecting all
> zero-sum subsets, which is NP-hard (subset-sum). This greedy approach achieves
> the n−1 bound in the general case and is the standard production approach.

## Technical decisions

- **`BigDecimal` everywhere for money.** `double` silently loses paise — ₹100 split
  three ways is the canonical failure. There's a test for exactly this case.
- **Fail loudly on invalid input.** Balances that don't net to zero mean the caller's
  data is wrong. Returning a plan that leaves money unaccounted for would be worse
  than a 400.
- **Property-based assertions over exact-output assertions.** The main test checks
  that every participant's net movement equals their starting balance, rather than
  asserting a specific transaction list — so the tests survive algorithm changes
  that are still correct.
- **Constructor injection, not field injection.** Dependencies are explicit and the
  class is testable without a Spring context.

## Running it

Requires **Java 21+**. No Maven install needed — the wrapper handles it.

```bash
git clone https://github.com/butterfly0404/fairshare-api.git
cd fairshare-api
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Health check: http://localhost:8080/actuator/health

## API

### `POST /api/v1/settlements/simplify`

Takes net balances, returns the minimum settlement plan.
Positive = owed money, negative = owes money. Must sum to zero.

```bash
curl -X POST http://localhost:8080/api/v1/settlements/simplify \
  -H "Content-Type: application/json" \
  -d '{"Alice": 500, "Bob": -300, "Carol": -200}'
```

```json
[
  { "from": "Bob",   "to": "Alice", "amount": 300 },
  { "from": "Carol", "to": "Alice", "amount": 200 }
]
```

Returns `400` with an error message if balances don't sum to zero.

## Testing

```bash
./mvnw test
```

9 tests covering the empty group, an already-settled group, the n−1 bound,
members with zero balance, money conservation, paise-level rounding remainders,
and rejection of unbalanced input.

## Roadmap

- [ ] Persist groups, members and expenses (PostgreSQL + Flyway)
- [ ] Split strategies: equal, exact amount, percentage
- [ ] JWT authentication
- [ ] OpenAPI / Swagger UI
- [ ] Docker Compose for one-command startup
- [ ] GitHub Actions CI

## Built with

Java 21 · Spring Boot 4.1.1 · Spring Data JPA · Hibernate 7 · JUnit 5 · AssertJ · Maven