# BudgetBuddy --- UI Specification

## Visual language

Modern finance SaaS.

Characteristics: - Professional. - Clean. - Responsive. - Strong
hierarchy. - Card-based dashboard. - Clear income/expense distinction. -
Accessible contrast. - Mobile-first responsive behavior. - Consistent
forms and validation.

## Authentication screens

### Login

Components: - Email - Password - Login button - Google Sign-In button -
Link to registration

No forgot-password link.

### Registration

Components: - Name - Email - Password - Confirm password - Register
button - Google Sign-In

## Dashboard

Sections: 1. Header/navigation. 2. Summary cards. 3. Budget warning when
over budget. 4. Income vs expense chart. 5. Savings/loss trend. 6.
Expense category breakdown. 7. Savings target progress. 8. Recent
transactions. 9. Highest spending categories.

Loading state: - Skeleton cards/charts.

Empty state: - Explain that no transactions exist and provide an Add
Transaction action.

Error state: - Explain failure and provide retry.

## Transactions

List view: - Date - Type - Category - Remarks - Amount - Actions

Controls: - Search. - Type filter. - Category filter. - Date range. -
Sort. - Pagination.

Create/edit form: - Type - Amount - Date - Category - Remarks

If category = Other: - Show custom category text input. - Require a
non-empty custom category.

## Recurring Finances

Show active recurring items.

Each item: - Name/category - Income/expense - Amount - Monthly
frequency - Start date - Status - Edit/deactivate controls

Creation form: - Type - Amount - Category - Start date - Remarks -
Active status

Clearly explain: "Changing this amount affects future monthly
transactions only."

## Budget/Profile

Allow: - Monthly budget. - Monthly savings target. - Name.

Show current values and save state.

## Chat

Chat interface: - Conversation area. - Message input. - Send. - Loading
indicator. - Retry on failed response. - Clear conversation if
implemented locally.

Display a brief informational disclaimer.

## Responsive behavior

Desktop: - Sidebar or compact navigation. - Multi-column dashboard
cards/charts.

Tablet: - Reduced columns.

Mobile: - Stacked cards. - Collapsible/mobile navigation. - Full-width
forms. - Charts remain readable without horizontal overflow.

## Accessibility

Use: - Semantic labels. - Keyboard navigation. - Visible focus. -
Accessible chart summaries where possible. - Error messages associated
with fields. - Buttons with clear accessible names.
