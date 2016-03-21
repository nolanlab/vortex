USE [master]
/****** Object:  Database [%db_name%]    Script Date: 10/04/2012 12:53:03 ******/
CREATE DATABASE [%db_name%] ON  PRIMARY 
( NAME = '%db_name%', FILENAME = '%file_path%\%db_name%.mdf' , SIZE = 4MB , MAXSIZE = UNLIMITED, FILEGROWTH = 10%) 
LOG ON ( NAME = '%db_name%_log', FILENAME = '%file_path%\%db_name%_log.ldf' , SIZE = 504KB , MAXSIZE = 2048GB , FILEGROWTH = 10%)
;

ALTER DATABASE [%db_name%] SET ANSI_NULL_DEFAULT OFF 
;

ALTER DATABASE [%db_name%] SET ANSI_NULLS OFF 
;

ALTER DATABASE [%db_name%] SET ANSI_PADDING OFF 
;

ALTER DATABASE [%db_name%] SET ANSI_WARNINGS OFF 
;

ALTER DATABASE [%db_name%] SET ARITHABORT OFF 
;

ALTER DATABASE [%db_name%] SET AUTO_CLOSE OFF 
;

ALTER DATABASE [%db_name%] SET AUTO_CREATE_STATISTICS ON 
;

ALTER DATABASE [%db_name%] SET AUTO_SHRINK ON 
;

ALTER DATABASE [%db_name%] SET AUTO_UPDATE_STATISTICS ON 
;

ALTER DATABASE [%db_name%] SET CURSOR_CLOSE_ON_COMMIT OFF 
;

ALTER DATABASE [%db_name%] SET CURSOR_DEFAULT  GLOBAL 
;

ALTER DATABASE [%db_name%] SET CONCAT_NULL_YIELDS_NULL OFF 
;

ALTER DATABASE [%db_name%] SET NUMERIC_ROUNDABORT OFF 
;

ALTER DATABASE [%db_name%] SET QUOTED_IDENTIFIER OFF 
;

ALTER DATABASE [%db_name%] SET RECURSIVE_TRIGGERS OFF 
;

ALTER DATABASE [%db_name%] SET  DISABLE_BROKER 
;

ALTER DATABASE [%db_name%] SET AUTO_UPDATE_STATISTICS_ASYNC OFF 
;

ALTER DATABASE [%db_name%] SET DATE_CORRELATION_OPTIMIZATION OFF 
;

ALTER DATABASE [%db_name%] SET TRUSTWORTHY OFF 
;

ALTER DATABASE [%db_name%] SET ALLOW_SNAPSHOT_ISOLATION OFF 
;

ALTER DATABASE [%db_name%] SET PARAMETERIZATION SIMPLE 
;

ALTER DATABASE [%db_name%] SET READ_COMMITTED_SNAPSHOT OFF 
;

;

ALTER DATABASE [%db_name%] SET  READ_WRITE 
;

ALTER DATABASE [%db_name%] SET RECOVERY SIMPLE 
;

ALTER DATABASE [%db_name%] SET  MULTI_USER 
;

ALTER DATABASE [%db_name%] SET PAGE_VERIFY CHECKSUM  
;

ALTER DATABASE [%db_name%] SET DB_CHAINING OFF; 

/****** Object:  Table [%db_name%].[dbo].[ParamNames]    Script Date: 10/04/2012 13:08:09 ******/
SET ANSI_NULLS ON
;

SET QUOTED_IDENTIFIER ON
;

SET ANSI_PADDING ON
;

CREATE TABLE [%db_name%].[dbo].[ParamNames](
	[ParamID] [int] NOT NULL,
	[ParamName] [varchar](max) NULL,
	[ParamNameShort] [varchar](255) NULL,
 CONSTRAINT [PK_ParamNames] PRIMARY KEY CLUSTERED 
(
	[ParamID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]


/****** Object:  Table [%db_name%].[dbo].[ParamSets]    Script Date: 10/04/2012 13:09:08 ******/

CREATE TABLE [%db_name%].[dbo].[ParamSets](
	[ParamSetID] [varchar](255) NOT NULL,
	[Parent] [varchar](255) NULL,
	[ID] [int] IDENTITY(1,1) NOT NULL,
 CONSTRAINT [PK_ParamSets] PRIMARY KEY CLUSTERED 
(
	[ParamSetID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]

;

/****** Object:  Table [%db_name%].[dbo].[ParamSetMembers]    Script Date: 10/04/2012 13:09:25 ******/

CREATE TABLE [%db_name%].[dbo].[ParamSetMembers](
	[ParamSetID] [varchar](255) NOT NULL,
	[ParamID] [int] NULL,
	[ID] [int] IDENTITY(1,1) NOT NULL
) ON [PRIMARY]

;

ALTER TABLE [%db_name%].[dbo].[ParamSetMembers]  WITH CHECK ADD  CONSTRAINT [FK_ParamSetMembers_ParamSets] FOREIGN KEY([ParamSetID])
REFERENCES [%db_name%].[dbo].[ParamSets] ([ParamSetID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[ParamSetMembers] CHECK CONSTRAINT [FK_ParamSetMembers_ParamSets]
;

ALTER TABLE [%db_name%].[dbo].[ParamSetMembers]  WITH CHECK ADD  CONSTRAINT [FK_ParamSets_ParamNames] FOREIGN KEY([ParamID])
REFERENCES [%db_name%].[dbo].[ParamNames] ([ParamID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[ParamSetMembers] CHECK CONSTRAINT [FK_ParamSets_ParamNames]
;

/****** Object:  Table [%db_name%].[dbo].[Datasets]    Script Date: 10/04/2012 12:57:20 ******/

CREATE TABLE [%db_name%].[dbo].[Datasets](
	[DatasetID] [varchar](255) NOT NULL,
	[ProfileSetTable] [varchar](255) NOT NULL,
	[ParamSetID] [varchar](255) NOT NULL,
	[ScoringTable] [varchar](255) NULL,
	[EntityType] [varchar](50) NULL,
 CONSTRAINT [PK_Datasets] PRIMARY KEY CLUSTERED 
(
	[DatasetID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]

;

ALTER TABLE [%db_name%].[dbo].[Datasets]  WITH CHECK ADD  CONSTRAINT [FK_Datasets_Datasets] FOREIGN KEY([DatasetID])
REFERENCES [%db_name%].[dbo].[Datasets] ([DatasetID])
;

ALTER TABLE [%db_name%].[dbo].[Datasets] CHECK CONSTRAINT [FK_Datasets_Datasets]
;

ALTER TABLE [%db_name%].[dbo].[Datasets]  WITH CHECK ADD  CONSTRAINT [FK_Datasets_ParamSets] FOREIGN KEY([ParamSetID])
REFERENCES [%db_name%].[dbo].[ParamSets] ([ParamSetID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[Datasets] CHECK CONSTRAINT [FK_Datasets_ParamSets]
;

/****** Object:  Table [%db_name%].[dbo].[DatasetMembers]    Script Date: 10/04/2012 13:02:44 ******/

CREATE TABLE [%db_name%].[dbo].[DatasetMembers](
	[DatasetID] [varchar](255) NOT NULL,
	[ProfileID] [varchar](255) NOT NULL,
	[ID] [bigint] NULL,
 CONSTRAINT [PK_DatasetMembers] PRIMARY KEY CLUSTERED 
(
	[DatasetID] ASC,
	[ProfileID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]

;

ALTER TABLE [%db_name%].[dbo].[DatasetMembers]  WITH CHECK ADD  CONSTRAINT [FK_DatasetMembers_Datasets] FOREIGN KEY([DatasetID])
REFERENCES [%db_name%].[dbo].[Datasets] ([DatasetID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[DatasetMembers] CHECK CONSTRAINT [FK_DatasetMembers_Datasets]
;


/****** Object:  Table [%db_name%].[dbo].[DatasetAnnotations]    Script Date: 10/04/2012 13:03:29 ******/

CREATE TABLE [%db_name%].[dbo].[DatasetAnnotations](
	[AnnotationID] [int] IDENTITY(1,1) NOT NULL,
	[DatasetID] [varchar](255) NOT NULL,
	[AnnotationName] [varchar](255) NOT NULL,
 CONSTRAINT [PK_DatasetAnnotations] PRIMARY KEY CLUSTERED 
(
	[AnnotationID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]

;

ALTER TABLE [%db_name%].[dbo].[DatasetAnnotations]  WITH CHECK ADD  CONSTRAINT [FK_DatasetAnnotations_DatasetAnnotations] FOREIGN KEY([AnnotationID])
REFERENCES [%db_name%].[dbo].[DatasetAnnotations] ([AnnotationID])
;

ALTER TABLE [%db_name%].[dbo].[DatasetAnnotations] CHECK CONSTRAINT [FK_DatasetAnnotations_DatasetAnnotations]
;

ALTER TABLE [%db_name%].[dbo].[DatasetAnnotations]  WITH CHECK ADD  CONSTRAINT [FK_DatasetAnnotations_Datasets] FOREIGN KEY([DatasetID])
REFERENCES [%db_name%].[dbo].[Datasets] ([DatasetID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[DatasetAnnotations] CHECK CONSTRAINT [FK_DatasetAnnotations_Datasets]
;

/****** Object:  Table [%db_name%].[dbo].[DatasetAnnotationMembers]    Script Date: 10/04/2012 13:04:36 ******/

CREATE TABLE [%db_name%].[dbo].[DatasetAnnotationMembers](
	[AnnotationID] [int] NOT NULL,
	[ProfileID] [varchar](255) NOT NULL,
	[Terms] [text] NOT NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

;

ALTER TABLE [%db_name%].[dbo].[DatasetAnnotationMembers]  WITH CHECK ADD  CONSTRAINT [FK_DatasetAnnotationMembers_DatasetAnnotations] FOREIGN KEY([AnnotationID])
REFERENCES [%db_name%].[dbo].[DatasetAnnotations] ([AnnotationID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[DatasetAnnotationMembers] CHECK CONSTRAINT [FK_DatasetAnnotationMembers_DatasetAnnotations]
;

/****** Object:  Table [%db_name%].[dbo].[ClusteringSessions]    Script Date: 10/04/2012 12:52:19 ******/

CREATE TABLE [%db_name%].[dbo].[ClusteringSessions](
	[ClusteringSessionID] [int] NOT NULL,
	[Date] [varchar](255) NULL,
	[DatasetID] [varchar](255) NULL,
	[ClusterNumber] [int] NULL,
	[BatchID] [int] NULL,
	[Caption] [varchar](max) NULL,
	[ColorCode] [varchar](20) NULL,
	[Comment] [text] NULL,
	[ClusteringAlgorithm] [varchar](255) NULL,
	[MainParameterValue] [float] NULL,
	[Parameters] [varchar](1000) NULL,
 CONSTRAINT [PK_ClusteringSessions] PRIMARY KEY CLUSTERED 
(
	[ClusteringSessionID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

;

ALTER TABLE [%db_name%].[dbo].[ClusteringSessions]  WITH CHECK ADD  CONSTRAINT [FK_ClusteringSessions_ClusteringSessions] FOREIGN KEY([DatasetID])
REFERENCES [%db_name%].[dbo].[Datasets] ([DatasetID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[ClusteringSessions] CHECK CONSTRAINT [FK_ClusteringSessions_ClusteringSessions]
;

/****** Object:  Table [%db_name%].[dbo].[Clusters]    Script Date: 10/04/2012 12:55:42 ******/

CREATE TABLE [%db_name%].[dbo].[Clusters](
	[ClusterID] [int] NOT NULL,
	[ClusteringSessionID] [int] NULL,
	[NumberOfElements] [int] NULL,
	[Caption] [varchar](max) NULL,
	[Comment] [text] NULL,
	[ColorCode] [text] NULL,	
 CONSTRAINT [PK_Clusters] PRIMARY KEY CLUSTERED 
(
	[ClusterID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]

;

ALTER TABLE [%db_name%].[dbo].[Clusters]  WITH CHECK ADD  CONSTRAINT [FK_Clusters_ClusteringSessions1] FOREIGN KEY([ClusteringSessionID])
REFERENCES [%db_name%].[dbo].[ClusteringSessions] ([ClusteringSessionID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[Clusters] CHECK CONSTRAINT [FK_Clusters_ClusteringSessions1]
;

/****** Object:  Table [%db_name%].[dbo].[ClusterMembers]    Script Date: 10/04/2012 12:56:55 ******/

CREATE TABLE [%db_name%].[dbo].[ClusterMembers](
	[ClusterID] [int] NOT NULL,
	[ProfileID] [varchar](255) NOT NULL,
	[DistanceToMode] [real] NULL,
	[Comment] [varchar](max) NULL,
	[ColorCode] [varchar](max) NULL,
	[PSS] [float] NULL,
	[Membership] [float] NULL,
 CONSTRAINT [PK_ClusterMembers] PRIMARY KEY NONCLUSTERED 
(
	[ClusterID] ASC,
	[ProfileID] ASC
)WITH (PAD_INDEX  = OFF, STATISTICS_NORECOMPUTE  = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS  = ON, ALLOW_PAGE_LOCKS  = ON) ON [PRIMARY]
) ON [PRIMARY]

;

SET ANSI_PADDING OFF
;

ALTER TABLE [%db_name%].[dbo].[ClusterMembers]  WITH CHECK ADD  CONSTRAINT [FK_ClusterMembers_Clusters] FOREIGN KEY([ClusterID])
REFERENCES [%db_name%].[dbo].[Clusters] ([ClusterID])
ON UPDATE CASCADE
ON DELETE CASCADE
;

ALTER TABLE [%db_name%].[dbo].[ClusterMembers] CHECK CONSTRAINT [FK_ClusterMembers_Clusters]
;