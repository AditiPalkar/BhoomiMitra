BHOOMIMITRA

BhoomiMitra is an integrated IoT and AI-based smart agriculture ecosystem designed to bridge the digital divide for modern farmers. Developed as a multi-modular Android application backed by an ESP32 microcontroller setup and Firebase Cloud Database, it combines precision farming, automated irrigation, AI diagnostics, and extensive digital utility services. The platform addresses common agricultural challenges like inefficient water usage, delayed crop disease response, lack of financial record-keeping, unorganized government scheme awareness, and market price fluctuations.

Key Features & Software ModulesCrop Monitoring: Displays real-time field data—soil moisture percentage, temperature, and humidity—fetched from cloud-connected hardware sensors.  
Smart Irrigation System: Runs dual-mode control. In Automatic Mode, it triggers the water pump via relay switching based on predefined soil moisture thresholds. In Manual Mode, farmers can override and toggle the pump on/off directly from the mobile app.  
AI-Based Crop Disease Prediction: Allows farmers to capture or upload a leaf photo to diagnose diseases instantly. It uses an image classification model (MobileNetV2 / CNN architecture converted via TensorFlow Lite) to output disease names, confidence scores, and recommended remedies/treatments.  
Market Price Update Module: Connects via APIs to show live Mandi/APMC market rates and trend analyses for various crops, helping farmers decide optimal selling times.   
Weather Forecast & Extreme Alerts: Fetches live weather API updates (temperature, rainfall, wind, humidity) and issues alerts for severe weather events like heavy rain or drought.  
Government Scheme Finder: A searchable directory where farmers can filter government schemes/subsidies by region or eligibility, view document requirements, and bookmark programs.  
Crop Diary & Expense Tracker: Allows log entry of farm activities and financial expenses (seeds, fertilizers, labor, transport) to calculate net profit/loss per crop cycle.   
Community Chat & Expert Help: Real-time discussion forum enabling farmers to ask questions, share crop images, and receive responses from experts or fellow farmers.   
Offline Knowledge Library: A locally stored database containing agricultural guides, pest management tutorials, and PDF manuals accessible without active internet connection.   
Voice Assistant & Multilingual Support: Includes regional language translation across the application interface alongside speech-to-text and text-to-speech interaction for non-technical users. 

Hardware Setup & Components
The hardware infrastructure operates in the field layer to collect environmental metrics and execute physical control:
ESP32 Development Board: Serves as the primary microcontroller; processes sensor readings and handles Wi-Fi synchronization with Firebase.
Soil Moisture Sensor: Inserted near the root zone to measure volumetric soil water content via analog signals.
DHT11 / DHT22 Sensor: Digital sensor measuring ambient temperature and atmospheric humidity.
Relay Module (5V/3.3V): Electrically operates as an automated switch between the low-power ESP32 controller and the high-power water pump circuit.
BC547 Transistor & LM2596 Step-Down Converter: Circuit components used for signal conditioning, switching driver logic, and regulated power conversion.
DC Water Pump: Provides water supply to the crops based on signals sent through the relay module.
Power Supply & Prototyping Gear: Standard adapter/USB power source, jumper wires, and breadboard/PCB for circuit connections.
