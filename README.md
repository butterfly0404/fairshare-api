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
> zero-sum subsets, which is NP-hard