# My Money Visualiser

This is a project to visualise Plain Text Accounting (PTA) Files.  See https://plaintextaccounting.org/ for details

Realistically this project works with my PTA files.  I make no guarantees about any others.

It reads a subset of the PTA file format and may also be compatible with a subset of other variations (hledger or Beancount)

It will make use of 3 files

## A plain text accounting ledger file that contains entries such as
```
2025/12/01 * Checking Opening balance
  Assets:Checking                   $1,000.00
  Equity:Cash:Opening Balances

2025/12/01 * Savings Opening balance
  Assets:Savings                   $10,000.00
  Equity:Cash:Opening Balances

2025/12/01 * Mortgage Opening balance
  Equity:Mortgage:Opening Balances          $100,000.00
  Liabilities:Mortgage:Principal
```

If your ledger file just uses the basic format then you should be ok.  Date format can be yyyy/MM/dd or yyyy-MM-dd

Entries can be open (no closing value) and will be calculated or closed (all values provided) and must sum to 0

commodities can be entered as well eg
```
2026/04/16 * ETF purchase
  assets:shares:VDHG  10  VDHG @ $75.5
  assets:bank:checking  $-755.00
2026/05/15 * Dividends received VDHG ; Needs code at end or start to link to share or fund
  assets:broker:schwabb  $128.40
  income:investment:distribution  $-128.40  ;Needs account to match with breakdown accounts
```

Note the second entry has the description ending in the commodity code.  This will allow linking events other than buy/sell with the commodity.

Your account entries should follow the convention of
Assets, Liabilities, Expenses, Income, Equity

case is not important.

## A commodities file for historical pricing of commodities with entries such as
```
P 2018/11/28 COL 12.85 AUD
P 2025/02/27 COL 19.68 AUD
P 2026/03/01 COL 21.36 AUD
```
## A schedules file for running schedules with entries such as
```
2026/05/20 Amazon Prime ; 1m
  Liabilities:Credit Card:NAB Visa  $-2.99
  Expenses:Cash:Household  $2.99
  
2026/06/01 Netflix ; 1m
  Liabilities:Credit Card:StGeorge Visa  $-20.99
  Expenses:Cash:Entertain  $20.99
```
This is the same format as a ledger file but the entry comment specifies the schedule period in d,w,m,y

Only the ledger file is mandatory.  For the other files if they are not supplied then some test data may be used.

This is a Java Application built using Java 21 so download the jar and using a java 21 runtime
```
java -jar mymoney.jar --ledger=myledger.ledger
```

The other files can be loaded with additional params

```
java -jar mymoney.jar --ledger=myledger.ledger --schedules=schedules.ledger --commodities=commodities.txt
```

There is also a demo docker image available using dummy data just ```docker run -p 8081:8081 dlmcpaul/mymoney:demo```

If the ledger file is read ok then a web page at http://localhost:8081/mymoney will be available with 9 pages

- DashBoard : Shows Balance Sheet (Assets & Liabilities), Scheduled Transactions, Monthly Profit & Loss along with Forms for ledger entry creation
- Accounts : Shows all open accounts (non-zero balance) as at the financial year specified
- Equity : Focuses on Equity accounts only
- Investments : A view of investment accounts
- Net Worth : A basic graph showing your net worth over the last 20 years
- Superannuation : A view of all superannuation accounts (open and closed)
- Recurring : List of schedules from the schedules file
- Taxation : A view based on Australian taxation
- Trends : Drill down into accounts over time to see trends

For these pages the **Dashboard, Accounts, Equity, Recurring, Trends** should work for most ledgers but the **Investments, Superannuation and Taxation** pages likely relies on hardcoded values specific to my ledger.

I may look to document these or try to make them configurable.

There are mechanisms to update the ledger file (accepting a schedule, posting a new journal) but you can primarily use it for visualisation.

![Screenshot](/docs/images/MyMoney-Dashboard.PNG)

![Screenshot](/docs/images/MyMoney-Accounts.PNG)
