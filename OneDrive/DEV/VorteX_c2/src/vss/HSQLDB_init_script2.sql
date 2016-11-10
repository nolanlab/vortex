drop schema vss if exists cascade;
create schema vss authorization DBA;
CREATE CACHED TABLE vss.config(
param varchar(255),
value other,
primary key (param)
);
CREATE CACHED TABLE vss.datasets(
id int,
dataset_name varchar(1024) unique,
date_created date,
last_access date,
num_points int,
feature_list other,
side_var_list other null,
primary key (id)
);
CREATE SEQUENCE vss.seq_dataset_id START WITH 1;

CREATE CACHED TABLE vss.sourcefiles(
id int,
dataset_id int,
filename varchar(1024),
primary key (id),
foreign key (dataset_id) references datasets(id) on delete cascade
);
CREATE SEQUENCE vss.seq_sourcefile_id START WITH 1;
CREATE INDEX vss.ix_sourcefiles_1 ON vss.sourcefiles (dataset_id);

CREATE CACHED TABLE vss.datapoints(
id int,
dataset_id int,
file_id int,
id_within_file int,
profile_name varchar(64) null,
vector other,
side_vector other null,
primary key (id),
foreign key (dataset_id) references datasets(id) on delete cascade
);
CREATE INDEX vss.ix_datapoints_1 ON vss.datapoints (dataset_id);
CREATE SEQUENCE vss.seq_dp_id START WITH 1;

CREATE CACHED TABLE vss.cluster_sets(
id int,
batch_id int,
dataset_id int,
date_created date,
clustering_method varchar(255),
num_clusters int,
comment varchar(1024),
color_code other,
ALGORITHMPARAMETERSTRING varchar(1024),
MAINPARAMETERVALUE double,
DISTANCE_MEASURE other,
primary key(id),
foreign key (dataset_id) references vss.datasets(id) on delete cascade
);
CREATE INDEX vss.ix_cluster_setss_1 ON vss.cluster_sets (dataset_id);
CREATE SEQUENCE vss.seq_cluster_sets_id START WITH 1;

CREATE CACHED TABLE vss.clusters(
id int,
cs_id int,
cluster_size int,
mode other, /*cluster mode object*/
caption varchar(1024),
comment varchar(1024),
color_code other null,
scores other null, /*cluster scores*/
cluster_member_indices other, /*int array of indices of cluster members*/
primary key (id),
foreign key (cs_id) references vss.cluster_sets(id) on delete cascade
);
CREATE INDEX vss.ix_clusters_1 ON vss.clusters (cs_id);
CREATE SEQUENCE vss.seq_clusters_id START WITH 1;

CREATE CACHED TABLE vss.annotations(
dataset_id int,
id int,
ann_name varchar(1024) unique,
primary key(id),
foreign key (dataset_id) references vss.datasets(id) on delete cascade
);
CREATE INDEX vss.ix_annotations_1 ON vss.annotations (dataset_id);
CREATE SEQUENCE vss.annotations_id START WITH 1;

CREATE CACHED TABLE vss.annotation_members(
annotation_id int,
dp_id int,
terms other, /*array of strings*/
foreign key (annotation_id) references vss.annotations(id) on delete cascade
);
CREATE INDEX vss.ix_annotation_members_1 ON vss.annotation_members (annotation_id);