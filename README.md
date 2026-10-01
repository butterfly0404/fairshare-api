# FairShare API

> Group expense tracking that settles everyone up in the **minimum number of payments**.
> Four people, four expenses, three split types — settled in 3 transactions.

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Tests](https://img.shields.io/badge/tests-9%20passing-success)

---

## The problem

When a group shares expenses — roommates, a trip, a team lunch rota — tracking who
owes whom gets messy fast. After 30 expenses across 6 people you have dozens of
small pairwise debts: A owes B ₹120, B owes C ₹120, C owes A ₹80.

Settling those naively means a payment for every debt. Most of them cancel out.
**What is the smallest set of payments that settles everyone?**

## What it does

Record expenses as they happen — who paid, how much, who shares it, and how it's
divided. FairShare nets each person down to a single balance and produces a
settlement plan with at most **n−1 transactions** for n people.

### Worked example

A four-person trip:

| Expense | Amount | Paid by | Split |
|---|---|---|---|
| Hotel, 2 nights | ₹12,000 | Alice | Equally, 4 ways |
| Beach shack dinner | ₹3,400 | Bob | Equally, 3 ways |
| Airport taxi | ₹2,000 | Carol | Exact: 800 / 700 / 500 |
| Scooter rental | ₹1,600 | Dave | By percentage: 50 / 25 / 25 |

Net balances:

```json
{ "Alice": 7400.00, "Bob": -1133.34, "Carol": -2833.33, "Dave": -3433.33 }
```

Settlement — **3 payments, not 10**:

| From | To | Amount |
|---|---|---|
| Dave | Alice | 3433.33 |
| Carol | Alice | 2833.33 |
| Bob | Alice | 1133.34 |

## How the algorithm works

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

**`BigDecimal` in Java, `NUMERIC(19,2)` in Postgres — never floating point.**
₹3,400 split three ways is 1133.333…; the shares come back as 1133.34 / 1133.33 /
1133.33, summing to exactly 3400. Rounding remainders are assigned deterministically
to the first participant rather than discarded. With `double` the group would quietly
lose money on every uneven split. There's a unit test for this case.

**Flyway owns the schema; Hibernate runs in `validate` mode.**
Migrations are hand-written SQL with foreign keys, check constraints and unique
constraints. `ddl-auto=update` is convenient and silently corrupts real databases.

**Balances are aggregated in SQL, not in Java.**
Two `GROUP BY` queries return one row per member regardless of whether the group
has ten expenses or ten thousand, instead of loading every split row into memory.

**Every `@ManyToOne` is explicitly `LAZY`, and no unbounded `@OneToMany` is mapped.**
JPA defaults `@ManyToOne` to `EAGER`, which is the usual source of N+1 query storms.
A group's expenses are queried through a repository rather than mapped as a
collection, so asking for a group's name can never drag 5,000 rows into memory.

**DTOs at the boundary, one exception handler for the whole API.**
Entities never reach the wire. A single `@RestControllerAdvice` means no controller
contains error handling and every error response has the same shape.

**Property-based tests over exact-output assertions.**
The main algorithm test asserts that every participant's net movement equals their
starting balance, rather than checking a specific transaction list — so the tests
survive algorithm changes that are still correct.

## Running it

Requires **Java 21+** and **Docker**. No Maven install needed — the project ships a wrapper.

```bash
git clone https://github.com/butterfly0404/fairshare-api.git
cd fairshare-api
docker compose up -d          # starts PostgreSQL 17
./mvnw spring-boot:run        # Windows: .\mvnw.cmd spring-boot:run
```

Flyway creates the schema on first start. Health check: http://localhost:8080/actuator/health

## API

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/api/v1/groups` | Create a group |
| `GET` | `/api/v1/groups/{id}` | Fetch a group |
| `POST` | `/api/v1/groups/{id}/members` | Add a member (reuses an existing user by email) |
| `GET` | `/api/v1/groups/{id}/members` | List members |
| `POST` | `/api/v1/groups/{id}/expenses` | Record an expense |
| `GET` | `/api/v1/groups/{id}/balances` | Net position per member |
| `GET` | `/api/v1/groups/{id}/settlements` | Minimum settlement plan |
| `POST` | `/api/v1/settlements/simplify` | Run the algorithm on balances directly |

### Recording an expense

`splitType` is `EQUAL`, `EXACT` or `PERCENTAGE`. `shares` is required for the
latter two, keyed by user id — exact amounts must sum to the expense total,
percentages must sum to 100.

```bash
curl -X POST http://localhost:8080/api/v1/groups/1/expenses \
  -H "Content-Type: application/json" \
  -d '{
        "description": "Beach shack dinner",
        "amount": 3400,
        "paidById": 2,
        "splitType": "EQUAL",
        "participantIds": [2, 3, 4]
      }'
```

### Trying the algorithm without creating data

```bash
curl -X POST http://localhost:8080/api/v1/settlements/simplify \
  -H "Content-Type: application/json" \
  -d '{"Alice": 500, "Bob": -300, "Carol": -200}'
```

### Errors

Consistent shape across the API: `400` for validation failures and broken
invariants (percentages not totalling 100, a non-member as payer), `404` for
unknown groups or users.

```json
{
  "timestamp": "2026-10-02T04:05:00Z",
  "status": 400,
  "error": "Bad Request",
  "detail": { "name": "Group name is required" }
}
```

## Testing

```bash
./mvnw test
```

9 tests covering the empty group, an already-settled group, the n−1 bound, members
with zero balance, money conservation, paise-level rounding remainders, and
rejection of balances that don't net to zero.

## Roadmap

- [ ] Integration tests against real PostgreSQL via Testcontainers
- [ ] JWT authentication and per-group authorization
- [ ] OpenAPI / Swagger UI
- [ ] Recording settlements as paid
- [ ] Multi-currency with daily FX rates
- [ ] GitHub Actions CI

## Built with

Java 21 · Spring Boot 4.1.1 · Spring Data JPA · Hibernate 7 · PostgreSQL 17 · Flyway · Docker · JUnit 5 · AssertJ · Maven
