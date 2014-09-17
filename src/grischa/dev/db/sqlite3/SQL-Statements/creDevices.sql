CREATE TABLE Devices (
    DeviceID INTEGER Primary Key,
    Name TEXT,
    Slot INTEGER NOT NULL,
    Rack INTEGER NOT NULL,
    IPAdresse TEXT NOT NULL,
    MaxConnections INTEGER,
    SerialNumber TEXT,
    OrderCode TEXT,
    Timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
    State INTEGER
);