💰 SaveIT — Personal Finance Management App

A JavaFX MVC-based Expense Tracker & Budgeting Tool

SaveIT is a JavaFX-powered personal finance management application designed to help users monitor their income, expenses, monthly budget, and financial habits. Built using the MVC architecture and MySQL for data persistence, SaveIT provides an intuitive and visually appealing interface to help students and young professionals take control of their financial wellbeing.

🚀 Features
✅ 1. User Authentication

Secure login & registration system with hashed credentials and database-stored profiles.

✅ 2. Transaction Management (CRUD)

Add, edit, delete, and view transactions:

Category

Amount

Type (Income / Expense)

Date

Notes

✅ 3. Expense Categorization

Automatically groups transactions into categories such as:

Food

Bills

Transportation

Entertainment

Savings

✅ 4. Analytics Dashboard

Includes data visualization using JavaFX charts:

Monthly income vs. expenses

Category spending distribution (Pie Chart)

History of expenses (Bar Chart)

✅ 5. Budget Alerts & Monthly Goals

Users can set monthly limits; the app warns them when:

They are nearing the limit

They exceeded the budget

🗂️ Extra Features

✔ Local database persistence
✔ Clean UI using JavaFX + custom CSS
✔ Modular MVC folder structure
✔ Scene switching and dynamic controllers

🛠️ Tech Stack
Category	Tools / Frameworks
Language	Java (JDK 17+)
UI Framework	JavaFX
Architecture	MVC
Database	MySQL
Libraries	ControlsFX / JFoenix (optional)
Tools	VS Code, SceneBuilder, GitHub
🏗️ MVC Architecture Overview
Model

Handles:

Database operations

User data

Transaction & Category classes

Budget logic

View

Built using FXML + CSS:

Login screen

Dashboard

Add Transaction

Reports

Controller

Manages:

UI actions

Validation

Data updates

Page transitions

🎯 Target Users

Students managing weekly allowance

Young professionals handling monthly salary

Household users tracking family expenses

Anyone who wants better control of their finances

📸 Screenshots / Demo

(Add images or a video link here once available)

📁 Setup Instructions
git clone https://github.com/your-username/saveit.git
cd saveit

Import into VS Code

Install the JavaFX extension pack

Set VM arguments (example):

--module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml

Import Database

Import the included .sql file into MySQL:

mysql -u root -p < saveit.sql

🙌 Developer

Jeane Eritch Diputado
BSCS — Object-Oriented Programming Project
TTH 7:30–10:00 • Mr. Acuin
