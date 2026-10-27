# Electricity Bill Web App (Java + HTML + CSS)

This project converts the original `ElectricBill.java` console program into a small local web application.

## Requirements
- JDK 17 or newer (JDK 21 is fine)
- Windows 10/11
- A web browser

No Maven, external Java libraries, or application server are required. The app uses Java's built-in `HttpServer`.

## Files
- `src/ElectricBillWebApp.java` — Java backend and original bill calculations
- `public/index.html` — HTML form and a small validation script
- `public/style.css` — responsive styling
- `run.bat` — compiles and starts the application on Windows

## Run it on Windows
1. Extract this folder to a location such as `C:\Users\YourName\Desktop\ElectricBillWebApp`.
2. Open the extracted `ElectricBillWebApp` folder.
3. Double-click `run.bat`.
   - Or open Command Prompt in this folder and run `run.bat`.
4. Wait until you see `Electricity Bill Web App is running.`
5. Open this address in your browser: http://localhost:8080
6. Fill out the form and select **Generate electricity bill**.
7. To stop the server, return to the Command Prompt window and press `Ctrl+C`.

If `run.bat` says Java is not recognized, make sure a JDK is installed and `java` and `javac` work in a new Command Prompt. You can check with `java -version` and `javac -version`.

## Billing logic preserved
- Units consumed = current reading − previous reading
- 0–100 units: ₹2.00 per unit
- 101–300 units: ₹2.00 for the first 100 units, then ₹3.00 per additional unit
- Above 300 units: first 100 at ₹2.00, next 200 at ₹3.00, remaining units at ₹5.00
- Fixed charge: ₹100.00
- Tax: 5% of energy charges
- Late fee: ₹50.00 when the user selects Yes; otherwise ₹0
- Bill date: current date; due date: 15 days after the bill date
- Negative readings, non-whole-number readings, missing fields, and current readings below previous readings are rejected.

The rate slabs are the sample rates in your original program, not a claim about any electricity provider's official tariff.

## Notes
- The web server listens on `localhost:8080` on your own computer.
- Keep the Command Prompt window running while using the app.
- If port 8080 is already in use, close the other app using it or change `8080` in `src/ElectricBillWebApp.java` and use the same port in your browser.
