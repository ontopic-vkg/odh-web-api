package io.vertx.web.api;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.api.JsonLdOptions;
import com.apicatalog.jsonld.api.impl.CompactionApi;
import com.apicatalog.jsonld.api.impl.FramingApi;
import com.apicatalog.jsonld.api.impl.FromRdfApi;
import com.apicatalog.jsonld.api.impl.ToRdfApi;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.apicatalog.rdf.RdfDataset;
import com.google.gson.JsonParser;
import com.moandjiezana.toml.Toml;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.jena.query.ParameterizedSparqlString;

import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class HttpServerVerticle extends AbstractVerticle {

    String jsonPath = "/api/JsonLD/DetailInLD";
    String host = "localhost";

    int port = 9090;

    public enum Type {

        accommodation;

    }

    @Override
    public void start() {

        System.out.println("Vert.x instance: " + vertx);


        Router router = Router.router(vertx);

        HttpServerOptions options = new HttpServerOptions()
                .setPort(port)
                .setHost(host);

        io.vertx.core.http.HttpServer server = vertx.createHttpServer(options);

        // TODO remove Test route
        router.route("/").handler(routingContext -> {

            HttpServerResponse response = routingContext.response();

            response.putHeader("content-type", "text/plain");

            response.end("hello !");
        });

        Route routeDetail = router.route(jsonPath);
        routeDetail.blockingHandler(handleDetailInLd());

        server.requestHandler(router);

        server.listen( res -> {
            if (res.succeeded()) {
                System.out.println("Server is now listening!");
            } else {
                System.out.println("Failed to bind!");
            }
        });
    }

    private Handler<RoutingContext> handleDetailInLd() {
        return routingContext -> {

            HttpServerRequest request = routingContext.request();

            // TODO remove Test
            System.out.println( "URI: " + request.uri() );
            System.out.println( "METHOD: " + request.method() );
            System.out.println( "VERSION: " + request.version() );
            System.out.println( "PATH: " + request.path() );
            System.out.println( "QUERY: " + request.query() );

            long startingTime = System.currentTimeMillis();

            HttpServerResponse response = routingContext.response();

            //Read input files
            InputStream tomlFile = getClass().getClassLoader().getResourceAsStream("construct-queries.toml");
            InputStream schema = getClass().getClassLoader().getResourceAsStream("schema-context.jsonld");
            InputStream jsonFramesStream = getClass().getClassLoader().getResourceAsStream("frames.json");

            Toml toml = new Toml().read(tomlFile);

            if (toml == null) {
                response.setStatusCode(500).end("Invalid toml file");
                return;
            }

            if (jsonFramesStream == null) {
                response.setStatusCode(500).end("Invalid json file");
                return;
            }
            Reader reader = new InputStreamReader(jsonFramesStream);
            com.google.gson.JsonObject frameObject = new JsonParser().parse(reader).getAsJsonObject();


            //TODO: localhost endpoint only for testing phase, remove once the endpoint is defined
            String endpointHost = request.getParam("endpoint") ;


            if (endpointHost == null) {
                endpointHost = "sparql.opendatahub.bz.it";
            }
            int endpointPort = endpointHost.equals("localhost")? 8080: 443;
            boolean isSSL = !endpointHost.equals("localhost");

            // Get query parameters
            String type = request.getParam("type");
            String id = request.getParam("Id");
            String language = request.getParam("language") ;
            String idtoshow = request.getParam("idtoshow") ;
            String urltoshow = request.getParam("urltoshow") ;
            String imageurltoshow = request.getParam("imageurltoshow") ;
            String showId = request.getParam("showid");

            if (type == null || id == null ){
                response.setStatusCode(400).end("The request" + request.uri() + " is invalid.");
            }
            if (urltoshow != null && !isValidURI(urltoshow)) {
                response.setStatusCode(400).end("Error: Invalid URI: The format of the URI could not be determined.");
            }
            if (imageurltoshow != null && !isValidURI(imageurltoshow)) {
                response.setStatusCode(400).end("Error: Invalid URI: The format of the URI could not be determined.");
            }

            WebClient client = WebClient.create(vertx);

            try {
                String query = getConstructSparqlQuery(toml, type);

                ParameterizedSparqlString pss = new ParameterizedSparqlString(query);
                pss.setLiteral("Id", id);
                if (language != null){
                    pss.setLiteral("language", language);
                }
                if (idtoshow != null){
                    pss.setIri("idtoshow", idtoshow);
                }
                if (urltoshow != null){
                    pss.setIri("urltoshow", urltoshow);
                }
                if (imageurltoshow != null){
                    pss.setIri("imageurltoshow", imageurltoshow);
                }
                if (showId != null){
                    pss.setLiteral("showid", showId);
                }

                String sparqlQuery = pss.toString();

                String frame = getFrame(frameObject, type);
                System.out.println("Start post request " + (System.currentTimeMillis() - startingTime));
                System.out.println(sparqlQuery);
                // Send a POST request
                client.post(endpointPort, endpointHost, "/sparql")
                        .ssl(isSSL)
                        .putHeader("Accept", "application/ld+json")
                        .putHeader("Content-type", "application/sparql-query")
                        .sendBuffer(Buffer.buffer(sparqlQuery), ar  -> {

                            System.out.println("On post request " + (System.currentTimeMillis() - startingTime));

                            if (ar.succeeded()) {
                                // Obtain response
                                HttpResponse<Buffer> res = ar.result();
                                String result = res.bodyAsString();

                                System.out.println("Received response with status code " + res.statusCode());
                                System.out.println("Returned jsonld " + result);

                                InputStream streamResult =  new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));
                                InputStream streamFrame =  new ByteArrayInputStream(frame.getBytes(StandardCharsets.UTF_8));

                                String responseJson = "";
                                try {
                                    JsonDocument jsonDocument = JsonDocument.of(streamResult);
                                    JsonDocument frameDoc = JsonDocument.of(streamFrame);

                                    JsonLdOptions  jlo = new JsonLdOptions();
//                                    jlo.setUseNativeTypes(true);
//                                    JsonArray rdf = JsonLd.fromRdf(RdfDocument.of(streamResult)).options(jlo).get();
//                                    JsonDocument jsonDocument = JsonDocument.of(rdf);

                                    FramingApi framedObject = JsonLd.frame(jsonDocument, frameDoc);
                                    if(schema != null) {
                                        JsonDocument streamSchema = JsonDocument.of(schema);
//                                        CompactionApi compactjson = JsonLd.compact(jsonDocument, streamDoc).options(jlo);
                                        jlo.setExpandContext(streamSchema);
                                    }
//                                        FramingApi framedObject = JsonLd.frame(JsonDocument.of(compactjson.get()), frameDoc);
                                    JsonObject framed = framedObject.options(jlo).get();
                                    responseJson = framed.toString();

                                    response.putHeader("content-type", "application/ld+json");
                                    System.out.println("Time " + (System.currentTimeMillis() - startingTime));
                                    response.end(responseJson);
                                } catch (JsonLdError jsonLdError) {
                                    jsonLdError.printStackTrace();
                                    response.setStatusCode(500).end("Problem processing in JsonLd " + jsonLdError.getMessage());
                                }


                            } else {
                                System.out.println("Something went wrong " + ar.cause().getMessage() );
                                response.setStatusCode(500).end("Something went wrong " + ar.cause().getMessage());

                            }
                        });

            } catch (Exception e) {
                response.setStatusCode(500).end("Something went wrong " + e.getMessage());
            }

        };
    }

    private boolean isValidURI(String input) {

        try {
            URL obj = new URL(input);
            obj.toURI();
            return true;
        } catch (Exception e1) {
            return false;
        }
    }

    private String getFrame(com.google.gson.JsonObject frameObject, String type) throws Exception {


        switch (Type.valueOf(type)) {
            case accommodation:
                return frameObject.get("accommodation").toString();

            default:
                throw  new Exception("Unsupported request");
        }

    }

    private String getConstructSparqlQuery(Toml toml, String type) throws Exception {

        switch (Type.valueOf(type)) {
            case accommodation:
                return toml.getString("accommodation.query");

            default:
                throw new Exception("Unsupported request");
        }

    }
}
