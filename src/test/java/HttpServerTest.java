import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
            // Send a request and get a response
//            HttpClient client = vertx.createHttpClient();
//
//            HttpClientRequest req = client.get(9090, "localhost", "/");
//            req.exceptionHandler(err -> context.fail(err.getMessage()));
//            req.handler(resp -> {
//                resp.bodyHandler(body -> context.assertEquals("hello !", body.toString("UTF-8")));
//                client.close();
//                async.complete();
//            });
//            req.end();

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

        suite.run(options);
    }


}
