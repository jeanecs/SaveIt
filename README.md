# 💰 **SaveIT — Personal Finance Management App**

### *A JavaFX MVC-Based Expense Tracker & Budgeting Tool*

SaveIT is a **JavaFX-powered personal finance application** that helps users track income, expenses, budgets, and financial habits. Built using **MVC architecture** with **MySQL persistence**, it offers a clean and intuitive UI designed for students and young professionals looking to take control of their finances.

---

## 🚀 **Features**

### ✅ **1. User Authentication**

* Secure login & registration
* Hashed passwords
* User profiles stored in MySQL

---

### ✅ **2. Transaction Management (CRUD)**

Add, edit, delete, and view transactions:

* Category
* Amount
* Type (Income / Expense)
* Date
* Notes

---

### ✅ **3. Expense Categorization**

Automatically groups spending into:

* 🍔 Food
* 💡 Bills
* 🚌 Transportation
* 🎮 Entertainment
* 💰 Savings

---

### ✅ **4. Analytics Dashboard**

Interactive charts powered by JavaFX:

* 📊 Monthly income vs expenses
* 🥧 Category spending (Pie Chart)
* 📉 Expense history (Bar Chart)

---

### ✅ **5. Budget Alerts & Monthly Goals**

The app notifies users when:

* They are **close to reaching their monthly budget**
* They **exceed their limit**

---

## 🗂️ **Extra Features**

* ✔ Local database persistence
* ✔ Clean and modern JavaFX UI
* ✔ Custom CSS styling
* ✔ MVC folder structure
* ✔ Scene switching with dynamic controllers

---

## 🛠 **Tech Stack**

| Category               | Tools / Frameworks                    |
| ---------------------- | ------------------------------------- |
| **Language**           | Java (JDK 17+)                        |
| **UI**                 | JavaFX                                |
| **Architecture**       | MVC                                   |
| **Database**           | MySQL                                 |
| **Optional Libraries** | ControlsFX, JFoenix                   |
| **Tools**              | VS Code / IntelliJ, SceneBuilder, Git |

---

## 🏗 **MVC Architecture Overview**

### **Model**

Handles:

* Database operations
* User data
* Transaction & Category classes
* Budget calculations

### **View**

Built using **FXML + CSS**:

* Login screen
* Dashboard
* Add Transaction
* Reports

### **Controller**

Responsible for:

* UI action handling
* Input validation
* Updating data/models
* Page transitions

---

## 🎯 **Target Users**

* Students tracking allowance
* Young professionals managing salary
* Families monitoring expenses
* Anyone improving financial awareness

---

## 📸 **Working UI**

Google Drive Demo Folder:
👉 [https://drive.google.com/drive/folders/1QERcFaMGS56sFyB3jqbzA5w7QpDEh7gp?usp=sharing](https://drive.google.com/drive/folders/1QERcFaMGS56sFyB3jqbzA5w7QpDEh7gp?usp=sharing)

---

# 📁 **Setup Instructions**

### **1. Clone the Repository**

```bash
git clone https://github.com/your-username/saveit.git
cd saveit
```

---

### **2. Import the Project**

Open in **VS Code** or **IntelliJ**.

If using VS Code:

* Install the **JavaFX Extension Pack**

---

### **3. Set VM Arguments**

Example run configuration:

```
--module-path /path/to/javafx/lib 
--add-modules javafx.controls,javafx.fxml
```

---

### **4. Import the Database**

Run the SQL file:

```bash
mysql -u root -p < saveit.sql
```

---

## 🙌 **Developer**

**Jeane Eritch Diputado**
BSCS — Object-Oriented Programming Project
TTH 7:30–10:00 • Instructor: Mr. Acuin


