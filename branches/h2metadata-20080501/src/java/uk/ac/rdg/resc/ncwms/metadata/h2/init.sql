/* Script to initialize the metadata database.  Will do nothing
   if the database already exists.  Note that some of these constructs are
   unique to the H2 database and hence this script may not be portable to other
   database systems.
   See http://www.h2database.com/html/grammar.html.

   Most tables have a foreign key reference to datasets.id with an
   "on delete cascade" clause, which ensures that, if a dataset is deleted, all
   of the metadata pertaining to this dataset is also deleted. */

create table if not exists datasets
(
    id varchar not null primary key,    /* Unique identifier, set in the
                                           configuration document */
    last_updated timestamp default null /* Time at which the metadata for this
                                           dataset was last updated */
);

/* Stores all types of spatial axis objects.  We could separate the different
   types of axis objects into different tables, but this would make it impossible
   to have foreign key relationships with the variables table. */
create table if not exists axes
(
    id identity not null primary key, /* auto-generated unique id */
    dataset_id varchar not null,      /* FK reference to datasets.id */
    axis_type varchar not null,       /* Lat, Lon, GeoX, GeoY etc */
    units varchar,                    /* Units of this axis (often ignored) */
    positive_up boolean,              /* Used only for z axes: true if values
                                         increase upwards */

    /* For regular axes (null otherwise) */
    axis_start double,                /* First value on this axis */
    axis_stride double,               /* Spacing between points on this axis */
    axis_count int,                   /* Number of points in this axis */

    /* For irregular axes (null otherwise) */
    axis_values other,                /* Array of values (usually doubles) on this axis */
                                      /* Note that the SQL array type doesn't seem to work in H2 */

    /* For look-up table axes (null otherwise) */
    lut_filename varchar,             /* The full path of the file containing the LUT data */

    constraint axes_fk1 foreign key (dataset_id) references datasets(id) on delete cascade
);

/* Stores HorizontalProjection objects via Java serialization (not very neat,
   should store parameters that define a horizontal projection) */
create table if not exists projections
(
    id identity not null primary key, /* auto-generated unique ID for this projection */
    dataset_id varchar not null,      /* FK reference to datasets.id */
    projection_object other not null, /* Stores the HorizontalProjection object */
    constraint projections_fk1 foreign key (dataset_id) references datasets(id) on delete cascade
);

/* This table exists to create a unique ID for each data file in the system.
   This saves space in the database because long filenames will not be used multiple
   times. */
create table if not exists data_files
(
    id identity not null primary key, /* auto-generated unique ID */
    dataset_id varchar not null,      /* FK reference to datasets.id */
    filepath varchar not null,        /* full path to the file (or NcML file or OPeNDAP location) */
    /* TODO: use last_modified and file size to check for changes to the file */
    constraint data_files_fk1 foreign key (dataset_id) references datasets(id) on delete cascade,
    /* A particular file path will be unique within a dataset */
    unique index data_files_idx1(dataset_id, filepath) 
);

/* This table contains details of the timesteps that are contained in each file. */
create table if not exists timesteps
(
    id identity not null primary key, /* auto-generated unique ID */
    data_file_id bigint not null,     /* FK reference to data_files.id */
    index_in_file int not null,       /* The index of this timestep in the file (starts at zero) */
    timestep timestamp not null,      /* The timestep as a date-time (millisecond accuracy) */
    constraint timesteps_fk1 foreign key (data_file_id) references data_files(id) on delete cascade
);
/* We will often be searching based on the timestep so we create an index */
/* We also search on data_file_id but this is a FK and already indexed by H2 */
create index if not exists timesteps_idx1 on timesteps(timestep);

/* The variables contained in the datasets */
create table if not exists variables
(
    id identity not null primary key,  /* auto-generated primary key for this variable */
    internal_name varchar not null,    /* internal id for the variable, as stored
                                          in the source data file(s) */
    dataset_id varchar not null,       /* FK reference to datasets.id */
    display_name varchar not null,     /* Human-readable name for display purposes */
    description varchar,               /* Description of this variable */
    units varchar,                     /* TODO: relate to javax.units? */

    bbox_west double not null,         /* The bounding box in lat-lon coordinates */
    bbox_south double not null,
    bbox_east double not null,
    bbox_north double not null,

    color_scale_min double,            /* Sensible minimum and maximum values for */
    color_scale_max double,            /* a colour scale */

    x_axis_id bigint,                  /* FK reference to axes.id */
    y_axis_id bigint,                  /* FK reference to axes.id */
    z_axis_id bigint,                  /* FK reference to axes.id */

    projection_id bigint,              /* FK reference to projections.id.  A null
                                          value signifies the default (lat-lon) projection */

    constraint variables_fk1 foreign key (dataset_id) references datasets(id) on delete cascade,
    constraint variables_fk2 foreign key (x_axis_id) references axes(id) on delete cascade,
    constraint variables_fk3 foreign key (y_axis_id) references axes(id) on delete cascade,
    constraint variables_fk4 foreign key (z_axis_id) references axes(id) on delete cascade,
    constraint variables_fk6 foreign key (projection_id) references projections(id) on delete cascade,
    /* This index helps searching on internal_name and dataset_id: good when
       trying to find a variable from its layer name */
    unique index variables_idx1(internal_name, dataset_id) 
);

/* Maps variables to their timesteps, defining a time dimension for the variable */
/* H2 will automatically create indices for variable_id and timestep_id because
   they are foreign keys */
create table if not exists variables_timesteps
(
    variable_id bigint not null, /* FK reference to variables.id */
    timestep_id bigint not null, /* FK reference to timesteps.id */
    constraint variables_timesteps_fk1 foreign key (variable_id) references variables(id) on delete cascade,
    constraint variables_timesteps_fk2 foreign key (timestep_id) references timesteps(id) on delete cascade,
    unique index variables_timesteps_idx1(variable_id, timestep_id) /* Pairings will be unique */
);
    