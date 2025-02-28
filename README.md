# **RTX Tracker**

## **What This Does**
RTX Tracker monitors the availability of **RTX 5080 graphics cards** on **MicroCenter’s** website. When a card is detected as **in stock**, it sends you an **SMS alert** using Twilio. You can also configure it to scan for any cards.

---

## **What You Need**
Ensure you have the following before proceeding:

- **Windows computer**
- **Java 23** (Required to run the program)
- **SBT 1.10.7** (Used to install and execute the program)
- **Twilio account** (Used to send SMS alerts)

---

## **How to Set It Up**

### **1. Install Java**
1. Download **Java 23** from [Oracle’s official website](https://www.oracle.com/java/technologies/javase/jdk23-archive-downloads.html).
2. Follow the installation instructions.

---

### **2. Install SBT (Scala Build Tool)**
1. Download **SBT 1.10.7** from [here](https://www.scala-sbt.org/download.html).
2. Follow the installation instructions.

---

### **3. Confirm Java and SBT are Installed**
1. Open **Command Prompt**:
   - Press **Windows + R**, type **cmd**, and press **Enter**.

2. Check Java version:  
   ```sh  
   java -version  
   ```  
   You should see output similar to this:  
   ```  
   java version "23.0.1" 2024-10-15  
   Java(TM) SE Runtime Environment (build 23.0.1+11-39)  
   Java HotSpot(TM) 64-Bit Server VM (build 23.0.1+11-39, mixed mode, sharing)  
   ```

3. Check SBT version:  
   ```sh  
   sbt -V  
   ```  
   Expected output:  
   ```  
   sbt version in this project: 1.10.7  
   sbt script version: 1.10.7  
   ```

---

### **4. Add Java and SBT to System Path (If Needed)**
If you get an error saying **"java is not recognized"** or **"sbt is not recognized"**, try restarting your PC. If the issue persists:

1. Open **Control Panel** > **System** > **Advanced system settings**.
2. Click on **Environment Variables**.
3. Under **System Variables**, find **Path**, select it, and click **Edit**.
4. Click **New**, then add the following directories:  
   ```  
   C:\Program Files\Java\jdk-23\bin  
   C:\Program Files\sbt\bin  
   ```
5. Click **OK** and restart your computer.

---

### **5. Download the Program**
1. Go to the **RTX Tracker** repository.
2. Click **Download ZIP**.
3. Extract the ZIP file to a folder on your computer.

---

### **6. Set Up Twilio**
1. **Create an account** at [Twilio](https://www.twilio.com/).
2. Get the following credentials from your Twilio dashboard:
   - **Account SID**
   - **Auth Token**
   - **Twilio Phone Number**

3. Open **Notepad** and create a new file named **.env** inside the program folder.
4. Copy and paste the following content, replacing placeholders with your Twilio details:  
   ```  
   TWILIO_ACCOUNT_SID=your_account_sid  
   TWILIO_AUTH_TOKEN=your_auth_token  
   TWILIO_FROM_NUMBER=your_twilio_phone_number  
   TO_NUMBER=your_phone_number  
   ```
5. **Save the file** inside the program directory.

---

### **7. Run the Program**
1. Open **Command Prompt** (Press **Windows + R**, type **cmd**, and press **Enter**).
2. Navigate to the program folder using **cd**, for example:  
   ```sh  
   cd C:\Users\YourName\RTXTracker  
   ```
3. Start SBT by typing:  
   ```sh  
   sbt  
   ```
4. Once inside the SBT shell, run the program:  
   ```sh  
   run  
   ```

---

## **Troubleshooting**
### **Java or SBT Not Recognized**
- Ensure Java and SBT are installed correctly.
- Try **restarting your computer**.
- Add Java and SBT to your system **Path** (see Step 4).

### **Not Receiving SMS Alerts**
- Double-check your **Twilio credentials** in the **.env** file.
- Ensure your **Twilio trial account** is configured correctly (if using a trial).
- Make sure the **TO_NUMBER** is verified in Twilio (for trial accounts).

---

That’s it! 🎉 **RTX Tracker** will now monitor GPU availability and send you an SMS when an RTX 5080 is in stock. 🚀  
