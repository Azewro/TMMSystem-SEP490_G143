# TMMSystem-SEP490_G143
# Towel Manufacturing Management System - Group 143 Software Engineering Capstone Project - FPT Universary
# Supervisor
Nguyễn Văn An - AnNV22@fe.edu.vn
# Team Members
Nguyễn Anh Hùng - HE180711 (Leader) - Full Stack Developer

Nguyễn Thị Thu Hiền - HE172532 (Member) - BA/Tester

Nguyễn Thị Thủy - HE172416 (Member) - BA/Tester

Đỗ Hải Phong - HE181907 (Member) - Full Stack Developer

Lê Văn Đức - HE160559 (Member) - Front-end Developer/Tester

# Project Overview

The Towel Manufacturing Management System (TMMS) is a software system for managing the towel manufacturing process, developed to overcome the limitations of manual management at My Duc Textile Company. Prior to the project, the company primarily tracked orders, plans, and production progress using Excel spreadsheets, text messages, and paper records, leading to a lack of synchronization between departments and a high risk of human error. The goal of TMMS is to automate the entire production process – from order placement to production planning and quality control – synchronizing data on a single platform and allowing real-time status monitoring, along with intuitive dashboards for management.

The TMMS system consists of two parts: front-end and back-end. The back-end (developed using Java Spring Boot, connected to a MySQL database) is primarily responsible for business logic processing, data storage, and providing APIs for the front-end. The back-end ensures the execution of user requests (via the front-end) such as order creation, production progress updates, etc., and integrates WebSocket to push real-time notifications to the front-end. This back-end system is deployed on Railway for convenient demo and practical operation.

# Key Features

*The TMMS back-end implements the main functions corresponding to the system's modules, providing APIs and services to the front-end as follows:*

- [Order Management: Handles the entire customer order lifecycle. The back-end provides APIs that allow creating Request for Quotations and generating quotes based on product information (towel type, quantity, special requirements, etc.). When the customer confirms, the system creates an order and stores it in the database. The back-end manages order status (in production, completed, etc.) and allows updates when the order progresses to a new stage. The front-end will call these APIs to send requests and display order information to users.]

- [Production Management: Performs production planning based on existing orders and factory capacity. The back-end handles the logic of dividing work into production stages (e.g., weaving, dyeing, finishing...), assigning each stage to the corresponding department, and calculating the expected start/end schedule. The system stores the progress of each stage and provides APIs to update production progress in real time. When a stage is completed or there is a change, the back-end can trigger an event (via WebSocket) to allow the front-end to update the interface promptly. Additionally, the back-end stores quality control results for each product batch at each stage, helping to standardize the quality management process.]

- [Equipment Management: Provides services for managing information on production machinery and equipment. The back-end maintains a list of equipment, its operational status, and the scheduled maintenance times for each machine. APIs are provided to create and update equipment information, schedule maintenance, and record when maintenance is complete. This allows the front-end to display the equipment list to the technical manager and notify them when maintenance is due or when equipment malfunctions.]

- [Notification Management: This function sends event notifications throughout the system. The back-end uses WebSocket (or STOMP via SocketJS if using Spring) to push real-time notifications to connected clients. When events such as new orders are created, production progress updates, or quality defects are detected, the back-end generates appropriate notification content and sends it to the front-end of the relevant users (e.g., a new order notification for the planning department). The front-end connected to WebSocket receives and displays these notifications instantly on the interface.]

- [System Administration: Manages user accounts and permissions. The back-end uses Spring Security (or an optional authentication method) to manage login credentials and permissions for roles such as admin, manager, planner, customer, etc. APIs are provided for admins to create/edit/delete users and assign roles; permission checks are also applied when users call other APIs (ensuring, for example, that only production managers have the right to update progress, customers can only view their own orders, etc.). The front-end calls login/logout APIs and performs administrative functions through the interface, while the back-end controls the entire authentication and authorization process.]
