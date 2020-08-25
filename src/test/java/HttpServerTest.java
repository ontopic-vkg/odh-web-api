import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestOptions;
import io.vertx.ext.unit.TestSuite;
import io.vertx.ext.unit.report.ReportOptions;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;


public class HttpServerTest {


    public static void main(String[] args) {
        new HttpServerTest().run();
    }

    Vertx vertx;

    public void run() {

        TestOptions options = new TestOptions().addReporter(new ReportOptions().setTo("console"));
        TestSuite suite = TestSuite.create("io.vertx.example.unit.test.HttpServerTest");

        suite.before(context -> {
            vertx = Vertx.vertx();
            vertx.deployVerticle("io.vertx.web.api.HttpServerVerticle", context.asyncAssertSuccess());
        });

        suite.after(context -> {
            vertx.close(context.asyncAssertSuccess());
        });

        suite.test("check connection to http server", context -> {

            Async async = context.async();

            WebClient client = WebClient.create(vertx);

            // Send a GET request
            client
                    .get(9090, "localhost", "/")
                    .send(ar -> {
                        if (ar.succeeded()) {
                            // Obtain response
                            HttpResponse<Buffer> response = ar.result();
                            context.assertEquals("hello !", response.bodyAsString("UTF-8"));
                            async.complete();
                            System.out.println("Received response with status code " + response.statusCode());
                        } else {
                            System.out.println("Something went wrong " + ar.cause().getMessage() );
                            context.fail(ar.cause().getMessage());
                        }
                    });


        });


        suite.test("web api parameters test", context -> {

            Async async = context.async();

            WebClient client = WebClient.create(vertx);

            // Send a GET request
            client
                    .get(9090, "localhost", "/api/JsonLD/DetailInLD?type=accommodation&Id=70043B17DAE33F1EFCDA24D4BB4C1F72&showid=true&endpoint=sparql.opendatahub.testingmachine.eu&language=de")
                    .send(ar -> {
                        if (ar.succeeded()) {
                            // Obtain response
                            HttpResponse<Buffer> response = ar.result();
                            JsonObject jsonresult = response.bodyAsJsonObject();
                            context.assertEquals(200, response.statusCode());
                            context.assertEquals("http://service.suedtirol.info/api/Accommodation/70043B17DAE33F1EFCDA24D4BB4C1F72", jsonresult.getString("id") );

                            async.complete();
                            System.out.println("Received response with status code " + response.statusCode());
                        } else {
                            System.out.println("Something went wrong " + ar.cause().getMessage() );
                            context.fail(ar.cause().getMessage());
                        }
                    });


        });

        suite.run(options);

    }




}
