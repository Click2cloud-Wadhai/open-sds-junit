package main.java.com.opensds.tests;

// how to get POJO from any response JSON, use this site
// http://pojo.sodhanalibrary.com/

import com.google.gson.Gson;
import jdk.jshell.execution.Util;
import main.java.com.opensds.HttpHandler;
import main.java.com.opensds.TestResultHTMLPrinter;
import main.java.com.opensds.Utils;
import main.java.com.opensds.jsonmodels.inputs.addbackend.AddBackendInputHolder;
import main.java.com.opensds.jsonmodels.inputs.addbackend.Backends;
import main.java.com.opensds.jsonmodels.inputs.addbackend.BackendsInputHolder;
import main.java.com.opensds.jsonmodels.inputs.createbucket.CreateBucketFileInput;
import main.java.com.opensds.jsonmodels.inputs.createmigration.*;
import main.java.com.opensds.jsonmodels.typesresponse.Type;
import main.java.com.opensds.jsonmodels.typesresponse.TypesHolder;
import main.java.com.opensds.utils.Constant;
import main.java.com.opensds.utils.TextUtils;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.swing.filechooser.FileView;
import java.io.File;
import java.io.FilePermission;
import java.io.IOException;
import java.nio.file.attribute.FileAttribute;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TestResultHTMLPrinter.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MigrationTests {

    public TypesHolder getTypesHolder() {
        return mTypesHolder;
    }

    public static HttpHandler getHttpHandler() {
        return mHttpHandler;
    }

    private static main.java.com.opensds.jsonmodels.authtokensresponses.AuthTokenHolder mAuthTokenHolder = null;
    private static TypesHolder mTypesHolder = null;
    private static HttpHandler mHttpHandler = new HttpHandler();

    @org.junit.jupiter.api.BeforeAll
    static void setUp() {
//        tTokenHolder tokenHolder = getHttpHandler().loginAndGetToken();
//        ProjectsHolder projectsHolder = getHttpHandler().getProjects(tokenHolder.getResponseHeaderSubjectToken(),
//                tokenHolder.getToken().getUser().getId());
//        mAuthTokenHolder = getHttpHandler().getAuthToken(tokenHolder.getResponseHeaderSubjectToken());
//        mTypesHolder = getHttpHandler().getTypes(getAuthTokenHolder().getResponseHeaderSubjectToken(),
//                getAuthTokenHolder().getToken().getProject().getId());
        mTypesHolder = getHttpHandler().getTypes(null, "adminTenantId");
    }

    @Test
    @Order(0)
    @DisplayName("Test creating bucket and backend on OPENSDS")
    public void testCreateBucketAndBackend() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_MIGRATION_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);
                int code = getHttpHandler().addBackend(null,
                        "adminTenantId",
                        inputHolder);
                assertEquals(code, 200);

                // backend added, now create buckets
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_MIGRATION_PATH);
                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
                        getAuthTokenHolder().getToken().getProject().getId());*/
                // create the bucket specified in each file
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));

                    // now create buckets
                    int cbCode = getHttpHandler().createBucket(null,
                            bfi, bName, null, "adminTenantId");//signatureKey);
                    System.out.println(cbCode);
                    assertEquals(cbCode, 200);
                    Response listBucketResponse = getHttpHandler().getBuckets(null, "adminTenantId");
                    boolean bucketFound = false;
                    try {
                        bucketFound = getHttpHandler()
                                .doesListBucketResponseContainBucketByName(listBucketResponse.body().string(), bName);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    assertTrue(bucketFound);
                }
            }
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test uploading object in a 1st bucket")
    public void testUploadObject() {
            List<File> listOfIBucketInputs =
                    Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                            Constant.CREATE_MIGRATION_PATH);
            assertNotNull(listOfIBucketInputs);
            File bucketFile = listOfIBucketInputs.get(0);
            System.out.println(bucketFile);
            // Get bucket name.
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            // Get object for upload.
            File fileRawData = new File(Constant.RAW_DATA_PATH);
            File[] files = fileRawData.listFiles();
            String mFileName = null;
            File mFilePath = null;
            for (File fileName : files) {
                mFileName = fileName.getName();
                mFilePath = fileName;

                int cbCode = getHttpHandler().uploadObject(null,
                        bucketName, mFileName, mFilePath);
                assertEquals("Uploaded object failed", cbCode, 200);

                //Verifying object is uploaded in bucket.
                Response listObjectResponse = getHttpHandler().getBucketObjects(bucketName);
                assertEquals("Get list of object failed", listObjectResponse.code(), 200);
                try {
                    JSONObject jsonObject = XML.toJSONObject(listObjectResponse.body().string());
                    JSONObject jsonObjectListBucket = jsonObject.getJSONObject("ListBucketResult");
                    if (jsonObjectListBucket.has("Contents")) {
                        if (jsonObjectListBucket.get("Contents") instanceof JSONArray){
                            JSONArray objects = jsonObjectListBucket.getJSONArray("Contents");
                            for (int i = 0; i < objects.length(); i++) {
                                assertEquals("Object is not uploaded", objects.getJSONObject(i).get("Key")
                                        , mFileName);
                            }
                        } else {
                            assertEquals("Object is not uploaded", jsonObjectListBucket
                                    .getJSONObject("Contents").get("Key"), mFileName);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
    }

    @Test
    @Order(2)
    @DisplayName("Test creating plan with immediately")
    public void testCreatePlan() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);
        System.out.println(json);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        assertEquals("Run plan creation failed: Response code not matched: ", codeRun, 200);
        String jobId = new JSONObject(jsonResRun).get("jobId").toString();

        Response responseGetJob = getHttpHandler().getJob(null, jobId,"adminTenantId");
        String jsonResGetJob = responseGetJob.body().string();
        int codeGetJob = responseGetJob.code();
        assertEquals("Get job id failed: Response code not matched: ", codeGetJob, 200);
        String status = new JSONObject(jsonResGetJob).getJSONObject("job").get("status").toString();
        System.out.println(status);
    }

    @Test
    @Order(3)
    @DisplayName("Test creating plan with immediately using empty request body")
    public void testCreatePlanUsingEmptyRequestBody() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName("");
        sourceConnInput.setStorType("");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName("");
        destConnInput.setStorType("");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName("");
        planeRequestInput.setDescription("");
        planeRequestInput.setType("");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);
        System.out.println(json);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        System.out.println(jsonRes);
        assertEquals("Plan creation failed request body empty: Response code not matched: ", code, 400);
    }

    @Test
    @Order(4)
    @DisplayName("Test after migration download image from source and destination bucket")
    public void testSourceDesBucketDownloadObject() {
        List<File> listOfIBucketInputs = Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        for (File bucketFile: listOfIBucketInputs) {
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            // Get object for upload.
            File fileRawData = new File(Constant.RAW_DATA_PATH);
            File[] files = fileRawData.listFiles();
            String mFileName = null;
            for (File fileName : files) {
                mFileName = fileName.getName();
            }
            String fileName = bucketName+"Migration_obj.jpg";
            File filePath = new File(Constant.DOWNLOAD_FILES_PATH);
            File downloadedFile = new File(Constant.DOWNLOAD_FILES_PATH, fileName);
            if (filePath.exists()) {
                if (downloadedFile.exists()) {
                    boolean isDownloadedFileDeleted = downloadedFile.delete();
                    assertTrue(isDownloadedFileDeleted, "Image deleting is failed");
                } else {
                    assertFalse(downloadedFile.exists());
                }
            } else {
                filePath.mkdirs();
            }
            int cbCode = getHttpHandler().downloadObject(null,
                    bucketName, mFileName, fileName);
            assertEquals("Downloading failed", cbCode, 200);
            assertTrue(downloadedFile.isFile(), "Downloaded Image is not available");
        }
    }

    @Test
    @Order(5)
    @DisplayName("Test re-creating plan with immediately using same name")
    public void testReCreatePlan() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        System.out.println(jsonRes);
        testGetPlansListAndDelete();
        assertEquals("Plan already created: Response code not matched: ", code, 409);
    }

    @Test
    @Order(6)
    @DisplayName("Test creating plan with immediately using invalid plan id")
    public void testCreatePlanInvalidPlanId() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");
        id = "234567887"; // Intercept with this value

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        System.out.println(jsonResRun);
        testGetPlansListAndDelete();
        assertEquals("Run plan creation failed with invalid id: Response code not matched: ", codeRun, 403);
    }

    @Test
    @Order(7)
    @DisplayName("Test creating plan with immediately using invalid job id")
    public void testCreatePlanInvalidJobId() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        assertEquals("Run plan creation failed: Response code not matched: ", codeRun, 200);
        String jobId = new JSONObject(jsonResRun).get("jobId").toString();
        assertNotNull(jobId,"Job id is null: ");
        jobId= "0384756565";

        Response responseGetJob = getHttpHandler().getJob(null, jobId,"adminTenantId");
        String jsonResGetJob = responseGetJob.body().string();
        int codeGetJob = responseGetJob.code();
        System.out.println(jsonResGetJob);
        testGetPlansListAndDelete();
        assertEquals("Job id may be valid: Response code not matched: ", codeGetJob, 403);
    }

    @Test
    @Order(8)
    @DisplayName("Test creating plan with immediately and delete the source objects after the migration is completed")
    public void testCreatePlanDeleteSourceObject() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(false);
        String json = gson.toJson(planeRequestInput);
        System.out.println("Source bucket: "+Utils.getBucketName(listOfIBucketInputs.get(0)));
        System.out.println("Destination bucket: "+Utils.getBucketName(listOfIBucketInputs.get(1)));

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        assertEquals("Run plan creation failed: Response code not matched: ", codeRun, 200);
        String jobId = new JSONObject(jsonResRun).get("jobId").toString();

        Response responseGetJob = getHttpHandler().getJob(null, jobId,"adminTenantId");
        String jsonResGetJob = responseGetJob.body().string();
        int codeGetJob = responseGetJob.code();
        assertEquals("Get job id failed: Response code not matched: ", codeGetJob, 200);
        String status = new JSONObject(jsonResGetJob).getJSONObject("job").get("status").toString();
        System.out.println(status);
    }

    @Test
    @Order(9)
    @DisplayName("Test delete the source objects after migration download image from destination bucket")
    public void testDesBucketDownloadObject() {
        List<File> listOfIBucketInputs = Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        for (File bucketFile: listOfIBucketInputs) {
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            // Get object for upload.
            File fileRawData = new File(Constant.RAW_DATA_PATH);
            File[] files = fileRawData.listFiles();
            String mFileName = null;
            for (File fileName : files) {
                mFileName = fileName.getName();
            }
            String fileName = bucketName+"Migration_obj.jpg";
            File filePath = new File(Constant.DOWNLOAD_FILES_PATH);
            File downloadedFile = new File(Constant.DOWNLOAD_FILES_PATH, fileName);
            if (filePath.exists()) {
                if (downloadedFile.exists()) {
                    System.out.println(" Download Image Path: "+downloadedFile);
                    boolean isDownloadedFileDeleted = downloadedFile.delete();
                    assertTrue(isDownloadedFileDeleted, "Image deleting is failed");
                } else {
                    assertFalse(downloadedFile.exists());
                }
            } else {
                filePath.mkdirs();
            }
            int cbCode = getHttpHandler().downloadObject(null,
                    bucketName, mFileName, fileName);
            assertTrue(cbCode == 200 || cbCode == 404, "Downloading failed: ");
            System.out.println("Bucket Name: "+bucketName+" Response Code: "+cbCode);
            if (cbCode == 200) {
                assertTrue(downloadedFile.isFile(), "Downloaded Image is not available");
            }

            if (cbCode == 404){
                assertFalse(downloadedFile.isFile(), "Downloaded Image is available");
            }
        }
    }

    @Test
    @Order(10)
    @DisplayName("Test get job list")
    public void testGetJobList() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(Utils.getRandomName("Plan_"));
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);
        System.out.println(json);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        assertEquals("Run plan creation failed: Response code not matched: ", codeRun, 200);
        String jobId = new JSONObject(jsonResRun).get("jobId").toString();

        Response responseJobList = getHttpHandler().getJobsList(null,"adminTenantId");
        String jsonResJobList = responseJobList.body().string();
        int codeJobList = responseJobList.code();
        System.out.println("Get Jobs List Response: "+jsonResJobList);
        assertEquals("Get Jobs List failed: Response code not matched: ", codeJobList, 200);
        JSONArray jsonArray = new JSONObject(jsonResJobList).getJSONArray("jobs");
        for (int i = 0; i < jsonArray.length(); i++) {
            String jobid = jsonArray.getJSONObject(i).get("id").toString();
            if (jobId.equals(jobid)){
                assertEquals("Job Id not matched: ", jobid, jobId);
            }
        }

        Response responseGetJob = getHttpHandler().getJob(null, jobId,"adminTenantId");
        String jsonResGetJob = responseGetJob.body().string();
        int codeGetJob = responseGetJob.code();
        assertEquals("Get job id failed: Response code not matched: ", codeGetJob, 200);
        String status = new JSONObject(jsonResGetJob).getJSONObject("job").get("status").toString();
        System.out.println(status);
    }

    @Test
    @Order(11)
    @DisplayName("Test get plans list and delete")
    public void testGetPlansListAndDelete() throws IOException {
        Response  response = getHttpHandler().getPlansList(null,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Get plans list failed: Response code not matched: ", code, 200);
        JSONArray jsonArray = new JSONObject(jsonRes).getJSONArray("plans");
        for (int i = 0; i < jsonArray.length(); i++) {
            String id = jsonArray.getJSONObject(i).get("id").toString();
            int codeRes = getHttpHandler().deletePlan(null,"adminTenantId" , id);
            assertEquals("Get plans list failed: Response code not matched: ", codeRes, 200);
        }
    }

    @Test
    @Order(12)
    @DisplayName("Test delete plan using invalid id")
    public void testDeletePlanUsingInvalidId() {
        int codeRes = getHttpHandler().deletePlan(null,"adminTenantId" , "1236456");
        assertEquals("Plan id may be valid: Response code not matched: ", codeRes, 403);
    }

    //@Test
    public void testDeleteBucketAndObject() {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_MIGRATION_PATH);
                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
                        getAuthTokenHolder().getToken().getProject().getId());*/
                // create the bucket specified in each file
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));

                    // now delete the object
                    int code = getHttpHandler().deleteObject(null,
                            "adminTenantId", bName, "Screenshot_1.jpg");
                    //System.out.println("Verifying object is deleted: "+code);
//                    assertEquals(code, 204);
//                    Response listObjectResponse = getHttpHandler().getBucketObjects(bName);
//                    assertEquals("Get list of object failed", listObjectResponse.code(), 200);
//                    JSONObject jsonObject = XML.toJSONObject(listObjectResponse.body().string());
//                    JSONObject jsonObjectListBucket = jsonObject.getJSONObject("ListBucketResult");
//                    if (jsonObjectListBucket.has("Contents")) {
//                        if (jsonObjectListBucket.get("Contents") instanceof JSONArray){
//                            JSONArray objects = jsonObjectListBucket.getJSONArray("Contents");
//                            for (int i = 0; i < objects.length(); i++) {
//                                assertNotEquals(objects.getJSONObject(i).get("Key").equals(bName),"Object is equal");
//                            }
//                        } else {
//                            assertNotEquals(jsonObjectListBucket.getJSONObject("Contents").get("Key").equals(bName),
//                                    "Object is equal");
//                        }
//                    } else {
//                        assertFalse(jsonObjectListBucket.has("Contents"));
//                    }

                    // now delete the bucket
                    int dbCode = getHttpHandler().deleteBucket(null,
                            "adminTenantId", bName);//signatureKey);
                    //System.out.println("Verifying bucket is deleted: "+dbCode);
                    //assertEquals(dbCode, 200);
                }
            }
        }
    }

    //@Test
    public void testDeleteBackend() throws IOException {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_BUCKET_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);
                AddBackendInputHolder inputHolder = gson.fromJson(content, AddBackendInputHolder.class);

                // Get backend list
                Response response = getHttpHandler().getBackends(null,
                        "adminTenantId");
                assertEquals(response.code(), 200);
                String responseBody = response.body().string();
                assertNotNull(responseBody);
                BackendsInputHolder backendsInputHolder = gson.fromJson(responseBody,
                        BackendsInputHolder.class);
                // Filter backend
                List<Backends> backendFilter = backendsInputHolder.getBackends().stream()
                        .filter(p -> !TextUtils.isEmpty(p.getName()))
                        .collect(Collectors.toList());

                List<Backends> backend = backendFilter.stream()
                        .filter(p -> p.getName().equals(inputHolder.getName()))
                        .collect(Collectors.toList());
                assertNotNull(backend);

                // Get backend
                for (int i = 0; i < backend.size(); i++) {
                    int responseCode = getHttpHandler().getDeleteBackend(null,
                            "adminTenantId", backend.get(i).getId());
                    //assertEquals(responseCode, 200);
                }
            }
        }
    }

    @Test
    @Order(13)
    @DisplayName("Test creating plan with schedule")
    public void testCreatePlanSchedule() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);

        Schedule schedule = new Schedule();
        schedule.setType("cron");
        assertNotNull(Constant.SCHEDULE_TIME);
        schedule.setTiggerProperties(Constant.SCHEDULE_TIME);
        PoliciesRequestInput policiesRequestInput = new PoliciesRequestInput();
        policiesRequestInput.setDescription("cron test function");
        policiesRequestInput.setName("cron test");
        policiesRequestInput.setTenant("all");
        policiesRequestInput.setSchedule(schedule);

        String jsonPolicies = gson.toJson(policiesRequestInput);
        System.out.println("Policies Json Req: "+jsonPolicies);

        Response  response = getHttpHandler().createPlanPolicies(null, jsonPolicies,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan policies failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("policy").get("id").toString();
        assertNotNull(id,"PolicyId is null: ");

        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneScheduleRequestInput  planeScheduleRequestInput = new PlaneScheduleRequestInput();
        planeScheduleRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeScheduleRequestInput.setDescription("for test");
        planeScheduleRequestInput.setType("migration");
        planeScheduleRequestInput.setSourceConn(sourceConnInput);
        planeScheduleRequestInput.setDestConn(destConnInput);
        planeScheduleRequestInput.setFilter(filter);
        planeScheduleRequestInput.setRemainSource(true);
        planeScheduleRequestInput.setPolicyId(id);
        planeScheduleRequestInput.setPolicyEnabled(true);
        String json = gson.toJson(planeScheduleRequestInput);
        System.out.println("Plan Json Req: "+json);

        Response responsePlan = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonResPlan = responsePlan.body().string();
        int codePlan = responsePlan.code();
        assertEquals("Plan creation failed: Response code not matched: ", codePlan, 200);
        String planName = new JSONObject(jsonResPlan).getJSONObject("plan").get("name").toString();

        Response responseSchedule = getHttpHandler().scheduleMigStatus(null,"adminTenantId", planName);
        int codeGetJob = responseSchedule.code();
        assertEquals("Schedule Mig Status failed: Response code not matched: ", codeGetJob, 200);
    }

    @Test
    @Order(14)
    @DisplayName("Test creating plan with schedule using same name")
    public void testCreatePlanScheduleUsingSameName() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);

        Schedule schedule = new Schedule();
        schedule.setType("cron");
        assertNotNull(Constant.SCHEDULE_TIME);
        schedule.setTiggerProperties(Constant.SCHEDULE_TIME);
        PoliciesRequestInput policiesRequestInput = new PoliciesRequestInput();
        policiesRequestInput.setDescription("cron test function");
        policiesRequestInput.setName("cron test");
        policiesRequestInput.setTenant("all");
        policiesRequestInput.setSchedule(schedule);

        String jsonPolicies = gson.toJson(policiesRequestInput);
        System.out.println("Policies Json Req: "+jsonPolicies);

        Response  response = getHttpHandler().createPlanPolicies(null, jsonPolicies,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan policies failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("policy").get("id").toString();
        assertNotNull(id,"PolicyId is null: ");

        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneScheduleRequestInput  planeScheduleRequestInput = new PlaneScheduleRequestInput();
        planeScheduleRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeScheduleRequestInput.setDescription("for test");
        planeScheduleRequestInput.setType("migration");
        planeScheduleRequestInput.setSourceConn(sourceConnInput);
        planeScheduleRequestInput.setDestConn(destConnInput);
        planeScheduleRequestInput.setFilter(filter);
        planeScheduleRequestInput.setRemainSource(true);
        planeScheduleRequestInput.setPolicyId(id);
        planeScheduleRequestInput.setPolicyEnabled(true);
        String json = gson.toJson(planeScheduleRequestInput);
        System.out.println("Plan Json Req: "+json);

        Response responsePlan = getHttpHandler().createPlans(null, json,"adminTenantId");
        int codePlan = responsePlan.code();
        assertEquals("Plan creation failed using same name: Response code not matched: ", codePlan, 409);
    }

    @Test
    @Order(15)
    @DisplayName("Test creating plan with schedule using yesterday date time")
    public void testCreatePlanScheduleUsingYesterdayDate() {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);

        Schedule schedule = new Schedule();
        schedule.setType("cron");
        String dateTime = "00 56 6 5 5 2";
        assertNotNull(dateTime);
        schedule.setTiggerProperties(dateTime);
        PoliciesRequestInput policiesRequestInput = new PoliciesRequestInput();
        policiesRequestInput.setDescription("cron test function");
        policiesRequestInput.setName("cron test");
        policiesRequestInput.setTenant("all");
        policiesRequestInput.setSchedule(schedule);
        String jsonPolicies = gson.toJson(policiesRequestInput);
        System.out.println("Policies Json Req: "+jsonPolicies);

        Response  response = getHttpHandler().createPlanPolicies(null, jsonPolicies,"adminTenantId");
        int code = response.code();
        assertEquals("Plan policies failed using yesterday date time: Response code not matched: ",
                code, 403);
    }

    @Test
    @Order(16)
    @DisplayName("Test creating plan with schedule and delete the source objects after the migration is completed")
    public void testCreatePlanScheduleDeleteSourceObject() throws IOException {
        testGetPlansListAndDelete();
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);

        Schedule schedule = new Schedule();
        schedule.setType("cron");
        assertNotNull(Constant.SCHEDULE_TIME);
        schedule.setTiggerProperties(Constant.SCHEDULE_TIME);
        PoliciesRequestInput policiesRequestInput = new PoliciesRequestInput();
        policiesRequestInput.setDescription("cron test function");
        policiesRequestInput.setName("cron test");
        policiesRequestInput.setTenant("all");
        policiesRequestInput.setSchedule(schedule);

        String jsonPolicies = gson.toJson(policiesRequestInput);
        System.out.println("Policies Json Req: "+jsonPolicies);

        Response  response = getHttpHandler().createPlanPolicies(null, jsonPolicies,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan policies failed:delete the source objects: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("policy").get("id").toString();
        assertNotNull(id,"PolicyId is null: ");

        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneScheduleRequestInput  planeScheduleRequestInput = new PlaneScheduleRequestInput();
        planeScheduleRequestInput.setName(listOfIBucketInputs.get(0).getName()+"-Plan");
        planeScheduleRequestInput.setDescription("for test");
        planeScheduleRequestInput.setType("migration");
        planeScheduleRequestInput.setSourceConn(sourceConnInput);
        planeScheduleRequestInput.setDestConn(destConnInput);
        planeScheduleRequestInput.setFilter(filter);
        planeScheduleRequestInput.setRemainSource(false);
        planeScheduleRequestInput.setPolicyId(id);
        planeScheduleRequestInput.setPolicyEnabled(true);
        String json = gson.toJson(planeScheduleRequestInput);
        System.out.println("Plan Json Req: "+json);

        Response responsePlan = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonResPlan = responsePlan.body().string();
        int codePlan = responsePlan.code();
        assertEquals("Plan creation failed: delete the source objects: Response code not matched: ", codePlan, 200);
        String planName = new JSONObject(jsonResPlan).getJSONObject("plan").get("name").toString();

        Response responseSchedule = getHttpHandler().scheduleMigStatus(null,"adminTenantId", planName);
        int codeGetJob = responseSchedule.code();
        assertEquals("Schedule Mig Status failed:delete the source objects: Response code not matched: ", codeGetJob, 200);
    }

    @Test
    @Order(17)
    @DisplayName("Test delete the source objects after schedule migration download image from destination bucket")
    public void testScheduleMigDesBucketDownloadObject() {
        List<File> listOfIBucketInputs = Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        for (File bucketFile: listOfIBucketInputs) {
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            // Get object for upload.
            File fileRawData = new File(Constant.RAW_DATA_PATH);
            File[] files = fileRawData.listFiles();
            String mFileName = null;
            for (File fileName : files) {
                mFileName = fileName.getName();
            }
            String fileName = "Schedule"+bucketName+"Migration_obj.jpg";
            File filePath = new File(Constant.DOWNLOAD_FILES_PATH);
            File downloadedFile = new File(Constant.DOWNLOAD_FILES_PATH, fileName);
            if (filePath.exists()) {
                if (downloadedFile.exists()) {
                    System.out.println(" Download Image Path: "+downloadedFile);
                    boolean isDownloadedFileDeleted = downloadedFile.delete();
                    assertTrue(isDownloadedFileDeleted, "Image deleting is failed");
                } else {
                    assertFalse(downloadedFile.exists());
                }
            } else {
                filePath.mkdirs();
            }
            int cbCode = getHttpHandler().downloadObject(null,
                    bucketName, mFileName, fileName);
            assertTrue(cbCode == 200 || cbCode == 404, "Downloading failed: ");
            System.out.println("Bucket Name: "+bucketName+" Response Code: "+cbCode);
            if (cbCode == 200) {
                assertTrue(downloadedFile.isFile(), "Downloaded Image is not available");
            }

            if (cbCode == 404){
                assertFalse(downloadedFile.isFile(), "Downloaded Image is available");
            }
        }
    }

    @Test
    @Order(18)
    @DisplayName("Test creating plan with schedule using invalid policy id")
    public void testCreatePlanScheduleInvalidPolicyId() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);

        Schedule schedule = new Schedule();
        schedule.setType("cron");
        assertNotNull(Constant.SCHEDULE_TIME);
        schedule.setTiggerProperties(Constant.SCHEDULE_TIME);
        PoliciesRequestInput policiesRequestInput = new PoliciesRequestInput();
        policiesRequestInput.setDescription("cron test function");
        policiesRequestInput.setName("cron test");
        policiesRequestInput.setTenant("all");
        policiesRequestInput.setSchedule(schedule);

        String jsonPolicies = gson.toJson(policiesRequestInput);
        System.out.println("Policies Json Req: "+jsonPolicies);

        Response  response = getHttpHandler().createPlanPolicies(null, jsonPolicies,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan policies failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("policy").get("id").toString();
        assertNotNull(id,"PolicyId is null: ");
        id = "1238234hjsjfdd";

        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(1)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Utils.getBucketName(listOfIBucketInputs.get(0)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneScheduleRequestInput  planeScheduleRequestInput = new PlaneScheduleRequestInput();
        planeScheduleRequestInput.setName(Utils.getRandomName("Plan_"));
        planeScheduleRequestInput.setDescription("for test");
        planeScheduleRequestInput.setType("migration");
        planeScheduleRequestInput.setSourceConn(sourceConnInput);
        planeScheduleRequestInput.setDestConn(destConnInput);
        planeScheduleRequestInput.setFilter(filter);
        planeScheduleRequestInput.setRemainSource(true);
        planeScheduleRequestInput.setPolicyId(id);
        planeScheduleRequestInput.setPolicyEnabled(true);
        String json = gson.toJson(planeScheduleRequestInput);
        System.out.println("Plan Json Req: "+json);

        Response responsePlan = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonResPlan = responsePlan.body().string();
        System.out.println("Plan Response Policy Disabled: "+jsonResPlan);
        int codePlan = responsePlan.code();
        assertEquals("Plan creation failed:Invalid policy id: Response code not matched: ", codePlan, 403);
    }

    @Test
    @Order(19)
    @DisplayName("Test get schedule status using non exist plan name")
    public void testGetScheduleStatusNonExistPlanName() throws IOException {
        Response responseSchedule = getHttpHandler().scheduleMigStatus(null,"adminTenantId", "hufsfh387646634567565567");
        String resp = responseSchedule.body().string();
        int codeGetJob = responseSchedule.code();
        System.out.println("Response non exist plan name: "+resp);
        assertEquals("Schedule Mig Status failed:non exist plan name: Response code not matched: ", codeGetJob, 404);
    }

    @Test
    @Order(20)
    @DisplayName("Test migration enable encryption on bucket")
    public void testEnableEncryptionOnBucket() throws IOException {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_MIGRATION_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                // backend added, now create buckets
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_MIGRATION_PATH);
                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
                        getAuthTokenHolder().getToken().getProject().getId());*/
                // create the bucket specified in each file
                for (File bucketFile : listOfIBucketInputs) {
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));
                    bName = Constant.BEGIN_NAME+bName;

                    // now create buckets
                    int cbCode = getHttpHandler().createBucket(null,
                            bfi, bName, null, "adminTenantId");//signatureKey);
                    System.out.println(cbCode);
                    assertEquals(cbCode, 200);

                    // now enable encryption on bucket
                    int responseCode = getHttpHandler().createEncryptionBucket(null,
                            bfi.getXmlRequestTrue(), bName, null, "adminTenantId");//signatureKey);
                    assertEquals(responseCode, 200);

                    Response listBucketResponse = getHttpHandler().getBuckets(null, "adminTenantId");
                    JSONObject jsonObject = XML.toJSONObject(listBucketResponse.body().string());
                    JSONArray jsonArray = jsonObject.getJSONObject("ListAllMyBucketsResult").getJSONArray("Buckets");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String name = jsonArray.getJSONObject(i).get("Name").toString();
                        if (!TextUtils.isEmpty(name)){
                            if (name.equals(bName)){
                                boolean isEncrypt = jsonArray.getJSONObject(i).getJSONObject("SSEConfiguration").getJSONObject("SSE")
                                        .getBoolean("enabled");
                                assertTrue(isEncrypt, "Not Encrypted: ");
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    @Order(21)
    @DisplayName("Test uploading object in a encryption enable bucket")
    public void testUploadObjectInEncryptionEnableBucket() {
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        File bucketFile = listOfIBucketInputs.get(0);
        System.out.println(bucketFile);
        // Get bucket name.
        String bucketContent = Utils.readFileContentsAsString(bucketFile);
        assertNotNull(bucketContent);
        String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                bucketFile.getName().indexOf("."));
        bucketName = Constant.BEGIN_NAME+bucketName;
        // Get object for upload.
        File fileRawData = new File(Constant.RAW_DATA_PATH);
        File[] files = fileRawData.listFiles();
        String mFileName = null;
        File mFilePath = null;
        for (File fileName : files) {
            mFileName = fileName.getName();
            mFilePath = fileName;

            int cbCode = getHttpHandler().uploadObject(null,
                    bucketName, mFileName, mFilePath);
            assertEquals("Uploaded object failed", cbCode, 200);

            //Verifying object is uploaded in bucket.
            Response listObjectResponse = getHttpHandler().getBucketObjects(bucketName);
            assertEquals("Get list of object failed", listObjectResponse.code(), 200);
            try {
                JSONObject jsonObject = XML.toJSONObject(listObjectResponse.body().string());
                JSONObject jsonObjectListBucket = jsonObject.getJSONObject("ListBucketResult");
                if (jsonObjectListBucket.has("Contents")) {
                    if (jsonObjectListBucket.get("Contents") instanceof JSONArray){
                        JSONArray objects = jsonObjectListBucket.getJSONArray("Contents");
                        for (int i = 0; i < objects.length(); i++) {
                            assertEquals("Object is not uploaded", objects.getJSONObject(i).get("Key")
                                    , mFileName);
                        }
                    } else {
                        assertEquals("Object is not uploaded", jsonObjectListBucket
                                .getJSONObject("Contents").get("Key"), mFileName);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @Order(22)
    @DisplayName("Test creating plan with immediately(Encryption enable bucket)")
    public void testCreatePlanUsingEncryptionEnabledBucket() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Constant.BEGIN_NAME+Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Constant.BEGIN_NAME+Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(Constant.BEGIN_NAME+listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);
        System.out.println(json);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        assertEquals("Run plan creation failed: Response code not matched: ", codeRun, 200);
        String jobId = new JSONObject(jsonResRun).get("jobId").toString();

        Response responseGetJob = getHttpHandler().getJob(null, jobId,"adminTenantId");
        String jsonResGetJob = responseGetJob.body().string();
        int codeGetJob = responseGetJob.code();
        assertEquals("Get job id failed: Response code not matched: ", codeGetJob, 200);
        String status = new JSONObject(jsonResGetJob).getJSONObject("job").get("status").toString();
        System.out.println(status);
    }

    @Test
    @Order(23)
    @DisplayName("Test after migration download image from source and destination encryption enable bucket")
    public void testSourceDesEncyEnableBucketDownloadObject() {
        List<File> listOfIBucketInputs = Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        for (File bucketFile: listOfIBucketInputs) {
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            bucketName = Constant.BEGIN_NAME+bucketName;
            // Get object for upload.
            File fileRawData = new File(Constant.RAW_DATA_PATH);
            File[] files = fileRawData.listFiles();
            String mFileName = null;
            for (File fileName : files) {
                mFileName = fileName.getName();
            }
            String fileName = Constant.BEGIN_NAME+bucketName+"Migration_obj.jpg";
            File filePath = new File(Constant.DOWNLOAD_FILES_PATH);
            File downloadedFile = new File(Constant.DOWNLOAD_FILES_PATH, fileName);
            if (filePath.exists()) {
                if (downloadedFile.exists()) {
                    boolean isDownloadedFileDeleted = downloadedFile.delete();
                    assertTrue(isDownloadedFileDeleted, "Image deleting is failed");
                } else {
                    assertFalse(downloadedFile.exists());
                }
            } else {
                filePath.mkdirs();
            }
            int cbCode = getHttpHandler().downloadObject(null,
                    bucketName, mFileName, fileName);
            assertEquals("Downloading failed", cbCode, 200);
            assertTrue(downloadedFile.isFile(), "Downloaded Image is not available");
        }
    }

    @Test
    @Order(24)
    @DisplayName("Test migration enable encryption on source bucket")
    public void testEnableEncryptionOnSourceBucket() throws IOException {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_MIGRATION_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                // backend added, now create buckets
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_MIGRATION_PATH);
                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
                        getAuthTokenHolder().getToken().getProject().getId());*/
                // create the bucket specified in each file
                    File bucketFile = listOfIBucketInputs.get(0);
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));
                    bName = Constant.SOURCE_ENABLE_NAME+bName;

                    // now create bucketsSOURCE_ENABLE_NAME
                    int cbCode = getHttpHandler().createBucket(null,
                            bfi, bName, null, "adminTenantId");//signatureKey);
                    System.out.println(cbCode);
                    assertEquals(cbCode, 200);

                    // now enable encryption on bucket
                    int responseCode = getHttpHandler().createEncryptionBucket(null,
                            bfi.getXmlRequestTrue(), bName, null, "adminTenantId");//signatureKey);
                    assertEquals(responseCode, 200);

                    Response listBucketResponse = getHttpHandler().getBuckets(null, "adminTenantId");
                    JSONObject jsonObject = XML.toJSONObject(listBucketResponse.body().string());
                    JSONArray jsonArray = jsonObject.getJSONObject("ListAllMyBucketsResult").getJSONArray("Buckets");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String name = jsonArray.getJSONObject(i).get("Name").toString();
                        if (!TextUtils.isEmpty(name)){
                            if (name.equals(bName)){
                                boolean isEncrypt = jsonArray.getJSONObject(i).getJSONObject("SSEConfiguration").getJSONObject("SSE")
                                        .getBoolean("enabled");
                                assertTrue(isEncrypt, "Not Encrypted: ");
                            }
                        }
                    }
            }
        }
    }

    @Test
    @Order(25)
    @DisplayName("Test uploading object in a encryption enable source bucket")
    public void testUploadObjectInEncryptionEnableSourceBucket() {
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        File bucketFile = listOfIBucketInputs.get(0);
        System.out.println(bucketFile);
        // Get bucket name.
        String bucketContent = Utils.readFileContentsAsString(bucketFile);
        assertNotNull(bucketContent);
        String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                bucketFile.getName().indexOf("."));
        bucketName = Constant.SOURCE_ENABLE_NAME+bucketName;
        // Get object for upload.
        File fileRawData = new File(Constant.RAW_DATA_PATH);
        File[] files = fileRawData.listFiles();
        String mFileName = null;
        File mFilePath = null;
        for (File fileName : files) {
            mFileName = fileName.getName();
            mFilePath = fileName;

            int cbCode = getHttpHandler().uploadObject(null,
                    bucketName, mFileName, mFilePath);
            assertEquals("Uploaded object failed", cbCode, 200);

            //Verifying object is uploaded in bucket.
            Response listObjectResponse = getHttpHandler().getBucketObjects(bucketName);
            assertEquals("Get list of object failed", listObjectResponse.code(), 200);
            try {
                JSONObject jsonObject = XML.toJSONObject(listObjectResponse.body().string());
                JSONObject jsonObjectListBucket = jsonObject.getJSONObject("ListBucketResult");
                if (jsonObjectListBucket.has("Contents")) {
                    if (jsonObjectListBucket.get("Contents") instanceof JSONArray){
                        JSONArray objects = jsonObjectListBucket.getJSONArray("Contents");
                        for (int i = 0; i < objects.length(); i++) {
                            assertEquals("Object is not uploaded", objects.getJSONObject(i).get("Key")
                                    , mFileName);
                        }
                    } else {
                        assertEquals("Object is not uploaded", jsonObjectListBucket
                                .getJSONObject("Contents").get("Key"), mFileName);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @Order(26)
    @DisplayName("Test creating plan with immediately(Encryption enable source bucket Destination bucket is not)")
    public void testCreatePlanUsingEncryptionEnabledSourceBucket() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Constant.SOURCE_ENABLE_NAME+Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Constant.SOURCE_ENABLE_NAME+Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(Constant.SOURCE_ENABLE_NAME+listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);
        System.out.println(json);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        assertEquals("Run plan creation failed: Response code not matched: ", codeRun, 200);
        String jobId = new JSONObject(jsonResRun).get("jobId").toString();

        Response responseGetJob = getHttpHandler().getJob(null, jobId,"adminTenantId");
        String jsonResGetJob = responseGetJob.body().string();
        int codeGetJob = responseGetJob.code();
        assertEquals("Get job id failed: Response code not matched: ", codeGetJob, 200);
        String status = new JSONObject(jsonResGetJob).getJSONObject("job").get("status").toString();
        System.out.println(status);
    }

    @Test
    @Order(27)
    @DisplayName("Test after migration download image from enable encryption on source bucket and destination is not")
    public void testSourceEncyEnableBucketDesIsNotDownloadObject() {
        List<File> listOfIBucketInputs = Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        for (File bucketFile: listOfIBucketInputs) {
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            bucketName = Constant.SOURCE_ENABLE_NAME+bucketName;
            // Get object for upload.
            File fileRawData = new File(Constant.RAW_DATA_PATH);
            File[] files = fileRawData.listFiles();
            String mFileName = null;
            for (File fileName : files) {
                mFileName = fileName.getName();
            }
            String fileName = Constant.SOURCE_ENABLE_NAME+bucketName+"Migration_obj.jpg";
            File filePath = new File(Constant.DOWNLOAD_FILES_PATH);
            File downloadedFile = new File(Constant.DOWNLOAD_FILES_PATH, fileName);
            if (filePath.exists()) {
                if (downloadedFile.exists()) {
                    boolean isDownloadedFileDeleted = downloadedFile.delete();
                    assertTrue(isDownloadedFileDeleted, "Image deleting is failed");
                } else {
                    assertFalse(downloadedFile.exists());
                }
            } else {
                filePath.mkdirs();
            }
            int cbCode = getHttpHandler().downloadObject(null,
                    bucketName, mFileName, fileName);
            assertEquals("Downloading failed", cbCode, 200);
            assertTrue(downloadedFile.isFile(), "Downloaded Image is not available");
        }
    }

    @Test
    @Order(28)
    @DisplayName("Test migration enable encryption on destination bucket")
    public void testEnableEncryptionOnDestinationBucket() throws IOException {
        // load input files for each type and create the backend
        for (Type t : getTypesHolder().getTypes()) {
            List<File> listOfIInputsForType =
                    Utils.listFilesMatchingBeginsWithPatternInPath(t.getName(),
                            Constant.CREATE_MIGRATION_PATH);
            Gson gson = new Gson();
            // add the backend specified in each file
            for (File file : listOfIInputsForType) {
                String content = Utils.readFileContentsAsString(file);
                assertNotNull(content);

                // backend added, now create buckets
                List<File> listOfIBucketInputs =
                        Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                                Constant.CREATE_MIGRATION_PATH);
                /*SignatureKey signatureKey = getHttpHandler().getAkSkList(getAuthTokenHolder().getResponseHeaderSubjectToken(),
                        getAuthTokenHolder().getToken().getProject().getId());*/
                // create the bucket specified in each file
                for (int j = 0; j < listOfIBucketInputs.size(); j++) {
                    File bucketFile = listOfIBucketInputs.get(j);
                    String bucketContent = Utils.readFileContentsAsString(bucketFile);
                    assertNotNull(bucketContent);

                    CreateBucketFileInput bfi = gson.fromJson(bucketContent, CreateBucketFileInput.class);

                    // filename format is "bucket_<bucketname>.json", get the bucket name here
                    String bName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                            bucketFile.getName().indexOf("."));
                    bName = Constant.DESTINATION_ENABLE_NAME+bName;

                    // now create bucketsSOURCE_ENABLE_NAME
                    int cbCode = getHttpHandler().createBucket(null,
                            bfi, bName, null, "adminTenantId");//signatureKey);
                    System.out.println(cbCode);
                    assertEquals(cbCode, 200);

                    // now enable encryption on bucket
                    if (j == 1){
                        System.out.println("Encry Enable Bucket: "+bName);
                        int responseCode = getHttpHandler().createEncryptionBucket(null,
                                bfi.getXmlRequestTrue(), bName, null, "adminTenantId");//signatureKey);
                        assertEquals(responseCode, 200);
                    }
                    Response listBucketResponse = getHttpHandler().getBuckets(null, "adminTenantId");
                    JSONObject jsonObject = XML.toJSONObject(listBucketResponse.body().string());
                    JSONArray jsonArray = jsonObject.getJSONObject("ListAllMyBucketsResult").getJSONArray("Buckets");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        String name = jsonArray.getJSONObject(i).get("Name").toString();
                        if (!TextUtils.isEmpty(name)){
                            if (name.equals(bName)){
                                boolean isEncrypt = jsonArray.getJSONObject(i).getJSONObject("SSEConfiguration").getJSONObject("SSE")
                                        .getBoolean("enabled");
                                if (j == 1) {
                                    assertTrue(isEncrypt, "Not Encrypted: ");
                                } else {
                                    assertFalse(isEncrypt, "Encrypted: ");
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    @Order(29)
    @DisplayName("Test uploading object in a encryption enable destination bucket")
    public void testUploadObjectInEncryptionEnableDestinationBucket() {
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        File bucketFile = listOfIBucketInputs.get(0);
        System.out.println(bucketFile);
        // Get bucket name.
        String bucketContent = Utils.readFileContentsAsString(bucketFile);
        assertNotNull(bucketContent);
        String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                bucketFile.getName().indexOf("."));
        bucketName = Constant.DESTINATION_ENABLE_NAME+bucketName;
        // Get object for upload.
        File fileRawData = new File(Constant.RAW_DATA_PATH);
        File[] files = fileRawData.listFiles();
        String mFileName = null;
        File mFilePath = null;
        for (File fileName : files) {
            mFileName = fileName.getName();
            mFilePath = fileName;

            int cbCode = getHttpHandler().uploadObject(null,
                    bucketName, mFileName, mFilePath);
            assertEquals("Uploaded object failed", cbCode, 200);

            //Verifying object is uploaded in bucket.
            Response listObjectResponse = getHttpHandler().getBucketObjects(bucketName);
            assertEquals("Get list of object failed", listObjectResponse.code(), 200);
            try {
                JSONObject jsonObject = XML.toJSONObject(listObjectResponse.body().string());
                JSONObject jsonObjectListBucket = jsonObject.getJSONObject("ListBucketResult");
                if (jsonObjectListBucket.has("Contents")) {
                    if (jsonObjectListBucket.get("Contents") instanceof JSONArray){
                        JSONArray objects = jsonObjectListBucket.getJSONArray("Contents");
                        for (int i = 0; i < objects.length(); i++) {
                            assertEquals("Object is not uploaded", objects.getJSONObject(i).get("Key")
                                    , mFileName);
                        }
                    } else {
                        assertEquals("Object is not uploaded", jsonObjectListBucket
                                .getJSONObject("Contents").get("Key"), mFileName);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    @Order(30)
    @DisplayName("Test creating plan with immediately(Encryption enable Destination bucket source bucket is not)")
    public void testCreatePlanUsingEncryptionEnabledDestinationBucket() throws IOException {
        Gson gson = new Gson();
        List<File> listOfIBucketInputs =
                Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                        Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        SourceConnInput sourceConnInput = new SourceConnInput();
        sourceConnInput.setBucketName(Constant.DESTINATION_ENABLE_NAME+Utils.getBucketName(listOfIBucketInputs.get(0)));
        sourceConnInput.setStorType("opensds-obj");
        DestConnInput destConnInput = new DestConnInput();
        destConnInput.setBucketName(Constant.DESTINATION_ENABLE_NAME+Utils.getBucketName(listOfIBucketInputs.get(1)));
        destConnInput.setStorType("opensds-obj");
        Filter filter = new Filter();
        PlaneRequestInput planeRequestInput = new PlaneRequestInput();
        planeRequestInput.setName(Constant.DESTINATION_ENABLE_NAME+listOfIBucketInputs.get(0).getName()+"-Plan");
        planeRequestInput.setDescription("for test");
        planeRequestInput.setType("migration");
        planeRequestInput.setSourceConn(sourceConnInput);
        planeRequestInput.setDestConn(destConnInput);
        planeRequestInput.setFilter(filter);
        planeRequestInput.setRemainSource(true);
        String json = gson.toJson(planeRequestInput);
        System.out.println(json);

        Response  response = getHttpHandler().createPlans(null, json,"adminTenantId");
        String jsonRes = response.body().string();
        int code = response.code();
        assertEquals("Plan creation failed: Response code not matched: ", code, 200);
        JSONObject jsonObject = new JSONObject(jsonRes);

        String id  = jsonObject.getJSONObject("plan").get("id").toString();
        assertNotNull(id,"Id is null: ");

        Response responseRun = getHttpHandler().runPlans(null, id,"adminTenantId");
        String jsonResRun = responseRun.body().string();
        int codeRun = responseRun.code();
        assertEquals("Run plan creation failed: Response code not matched: ", codeRun, 200);
        String jobId = new JSONObject(jsonResRun).get("jobId").toString();

        Response responseGetJob = getHttpHandler().getJob(null, jobId,"adminTenantId");
        String jsonResGetJob = responseGetJob.body().string();
        int codeGetJob = responseGetJob.code();
        assertEquals("Get job id failed: Response code not matched: ", codeGetJob, 200);
        String status = new JSONObject(jsonResGetJob).getJSONObject("job").get("status").toString();
        System.out.println(status);
    }

    @Test
    @Order(31)
    @DisplayName("Test after migration download image from enable encryption on destination bucket and source is not")
    public void testDestinationEncyEnableBucketSourceIsNotDownloadObject() {
        List<File> listOfIBucketInputs = Utils.listFilesMatchingBeginsWithPatternInPath("bucket",
                Constant.CREATE_MIGRATION_PATH);
        assertNotNull(listOfIBucketInputs);
        for (File bucketFile: listOfIBucketInputs) {
            String bucketContent = Utils.readFileContentsAsString(bucketFile);
            assertNotNull(bucketContent);
            String bucketName = bucketFile.getName().substring(bucketFile.getName().indexOf("_") + 1,
                    bucketFile.getName().indexOf("."));
            bucketName = Constant.DESTINATION_ENABLE_NAME+bucketName;
            // Get object for upload.
            File fileRawData = new File(Constant.RAW_DATA_PATH);
            File[] files = fileRawData.listFiles();
            String mFileName = null;
            for (File fileName : files) {
                mFileName = fileName.getName();
            }
            String fileName = Constant.DESTINATION_ENABLE_NAME+bucketName+"Migration_obj.jpg";
            File filePath = new File(Constant.DOWNLOAD_FILES_PATH);
            File downloadedFile = new File(Constant.DOWNLOAD_FILES_PATH, fileName);
            if (filePath.exists()) {
                if (downloadedFile.exists()) {
                    boolean isDownloadedFileDeleted = downloadedFile.delete();
                    assertTrue(isDownloadedFileDeleted, "Image deleting is failed");
                } else {
                    assertFalse(downloadedFile.exists());
                }
            } else {
                filePath.mkdirs();
            }
            int cbCode = getHttpHandler().downloadObject(null,
                    bucketName, mFileName, fileName);
            assertEquals("Downloading failed", cbCode, 200);
            assertTrue(downloadedFile.isFile(), "Downloaded Image is not available");
        }
    }
}
