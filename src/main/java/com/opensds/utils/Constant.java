package main.java.com.opensds.utils;

public class Constant {
    public static final  String PATH = System.getenv("INPUT_PATH");
    public static final  String CREATE_BUCKET_PATH = PATH+"inputs/createbucket";
    public static final  String CREATE_LIFECYCLE_PATH = PATH+"inputs/createlifecycle";
    public static final  String RAW_DATA_PATH = PATH+"inputs/rawdata";
    public static final  String DOWNLOAD_FILES_PATH = PATH+"inputs/download";
    public static final  String DELETE_BUCKET_PATH = PATH+"inputs/deletebucket";
    public static final  String EMPTY_FIELD_PATH = PATH+"inputs/emptyfield/";
    public static final  String CREATE_MIGRATION_PATH = PATH+"inputs/createmigration";
    // https://savvytime.com/converter/utc-to-ist
    // 00    56 UTC time     6       5          5             2
    //sec   mint            hr      date       Monthcount  Day count
    public static final  String SCHEDULE_TIME = System.getenv("SCHEDULE_TIME");
    public static final  String  BEGIN_NAME = "ecr-";
    public static final  String  SOURCE_ENABLE_NAME = "s-"+BEGIN_NAME;
    public static final  String  DESTINATION_ENABLE_NAME = "d-"+BEGIN_NAME;
}