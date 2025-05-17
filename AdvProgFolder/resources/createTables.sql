-- Reset DB
DROP TABLE IF EXISTS Airline;
DROP TABLE IF EXISTS Delay_Reason;
DROP TABLE IF EXISTS Flight;
DROP TABLE IF EXISTS Airport;

-- Airport table
CREATE TABLE Airport (
    iata_code CHAR(3) PRIMARY KEY,
    name TEXT NOT NULL
);

-- Airline table
CREATE TABLE Airline (
    iata_code CHAR(2) PRIMARY KEY, -- should be char(2) but may have suffix for multiple users in the form FLL(1) so is it 3? lef tthis part out. 
    name TEXT NOT NULL UNIQUE
);

-- Flight table
-- Flight table
CREATE TABLE Flight (
    flight_id INTEGER PRIMARY KEY, -- REMOVE AUTOINCREMENT DO IN JAVA (NEED TO CYCLE GENERATED KEYS)
    date CHAR(10) NOT NULL, -- Spec says char(8) but stored as 'YYYY-MM-DD' total 10
    airline_code CHAR(2) NOT NULL,
    flight_number INTEGER NOT NULL,
    flight_origin CHAR(3) NOT NULL,
    flight_destination CHAR(3) NOT NULL,
    scheduled_departure INTEGER,
    actual_departure INTEGER,
    scheduled_arrival INTEGER,
    actual_arrival INTEGER,
    
    FOREIGN KEY (flight_origin) REFERENCES airport(iata_code),
    FOREIGN KEY (flight_destination) REFERENCES airport(iata_code),
    FOREIGN KEY (airline_code) REFERENCES airline(iata_code)
);

CREATE TABLE Delay_Reason (
    delay_id INTEGER PRIMARY KEY AUTOINCREMENT,
    flight_id INTEGER NOT NULL UNIQUE,
    reason TEXT,
    delay_length INTEGER,
    
    FOREIGN KEY (flight_id) REFERENCES flight(flight_id)
);


