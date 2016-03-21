drop schema vss if exists cascade;
Create schema vss authorization DBA;
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

CREATE CACHED TABLE vss.datapoints(
id int,
dataset_id int,
profile_name varchar(1024),
primary_tag varchar(255),
vector other,
side_vector other null,
primary key (id),
foreign key (dataset_id) references datasets(id) on delete cascade
);
CREATE INDEX vss.ix_datapoints_1 ON vss.datapoints (dataset_id);
CREATE INDEX vss.ix_datapoints_profile_name ON vss.datapoints (profile_name);
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
color_code other,
scores other, /*cluster scores*/
primary key(id),
foreign key (cs_id) references vss.cluster_sets(id) on delete cascade
);
CREATE INDEX vss.ix_clusters_1 ON vss.clusters (cs_id);
CREATE SEQUENCE vss.seq_clusters_id START WITH 1;

CREATE CACHED TABLE vss.cluster_members(
id int,
cluster_id int,
datapoint_id int,
scores other, /*cluster member scores*/
comment varchar(255),
primary key(id),
foreign key (cluster_id) references vss.clusters(id) on delete cascade
);
CREATE INDEX vss.ix_cluster_members_1 ON vss.cluster_members (cluster_id);
CREATE SEQUENCE vss.seq_cluster_members_id START WITH 1;

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

CREATE CACHED TABLE vss.voronoi_cells(
dataset_id int,
dp_id int,
neighbor_ids other, /*voronoi neighbors, int[] dp_ids*/
point_count double, /*number of points within the cell*/
volume double, /*volume of the cell*/
distance_measure varchar(255), /*distance measure name*/
foreign key (dataset_id) references vss.datasets(id) on delete cascade
);
CREATE INDEX vss.ix_voronoi_cells_distance_measure ON vss.voronoi_cells (distance_measure);
CREATE INDEX vss.ix_voronoi_cells_dpid ON vss.voronoi_cells (dp_id);
CREATE INDEX vss.ix_voronoi_cells_3 ON vss.voronoi_cells (dataset_id);

CREATE CACHED TABLE vss.densities(
         dataset_id int,
         density_array other, 
         kernel varchar(255),
         foreign key (dataset_id) references vss.datasets(id) on delete cascade
);
CREATE INDEX vss.ix_densities_kernel ON vss.densities (kernel);
CREATE INDEX vss.ix_densities_2 ON vss.densities(dataset_id);

CREATE CACHED TABLE vss.distances(
DATASET_ID int,
dp_id int,
distance_array other, /*distance_array, double[]*/
distance_measure varchar(255), /*distance measure name*/
triangular boolean, /*specifies whether the array is a part of triangular matrix*/
foreign key (dataset_id) references vss.datasets(id) on delete cascade
);
CREATE INDEX vss.ix_distances_1 ON vss.distances(dataset_id);
CREATE INDEX vss.ix_distances_2 ON vss.distances(dp_id);
CREATE INDEX vss.ix_distances_3 ON vss.distances(distance_measure);

CREATE CACHED TABLE vss.sorted_dp(
dataset_id int,
dp_id int,
sorted_dp_array other, /*array of sorted dpIDs int[], descending order of proximity*/
distance_measure varchar(255), /*distance measure name*/
foreign key (dataset_id) references vss.datasets(id) on delete cascade
);
CREATE INDEX vss.ix_sorted_dp_1 ON vss.distances(distance_measure);
CREATE INDEX vss.ix_sorted_dp_2 ON vss.distances(dp_id);
CREATE INDEX vss.ix_sorted_dp_3 ON vss.distances(dataset_id);