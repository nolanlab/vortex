CREATE TABLE BandwidthArrays(
	DatasetID varchar(255) NOT NULL,
	BwSelectionMethod varchar(255) NOT NULL,
	BWArray blob NOT NULL,
 CONSTRAINT PK_BandwidthArrays PRIMARY KEY
(
	DatasetID,
	BwSelectionMethod
));

CREATE TABLE DensityArraysPrefetch(
	DatasetID varchar(255) NOT NULL,
	Kernel varchar(255) NOT NULL,
	DensityArray blob NOT NULL,
 CONSTRAINT PK_DensityArraysPrefetch PRIMARY KEY
(
	DatasetID,
	Kernel
)
);

CREATE TABLE InTessSimple(
	DatasetID varchar(255) NOT NULL,
	ProfileID varchar(255) NOT NULL,
	DatapointsInTess blob NULL,
	flag int NULL,
 CONSTRAINT PK_InTessSimple PRIMARY KEY 
(
	DatasetID,
	ProfileID
));

CREATE TABLE SortedArraysPrefetch(
	DatasetID varchar(255) NOT NULL,
	ProfileID varchar(255) NOT NULL,
	SerializedArray blob NOT NULL,
 CONSTRAINT PK_SortedArraysPrefetch PRIMARY KEY
(
	DatasetID,
	ProfileID
));

CREATE TABLE VoronoiCells(
	DatasetID varchar(255) NOT NULL,
	DpID int NOT NULL,
	VoronoiCell blob NULL,
	centRound int NOT NULL,
 CONSTRAINT PK_VoronoiCells PRIMARY KEY
(
	DatasetID,
	DpID,
	centRound
));
