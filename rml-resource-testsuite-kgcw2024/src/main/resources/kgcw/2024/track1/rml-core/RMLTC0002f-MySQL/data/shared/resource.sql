USE db;
DROP TABLE IF EXISTS db.Student;
CREATE TABLE Student (
ID INTEGER,
Name VARCHAR(15)
);
INSERT INTO Student (ID, Name) VALUES(10,'Venus');
