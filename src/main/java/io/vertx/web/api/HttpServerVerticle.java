package io.vertx.web.api;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.api.JsonLdError;
import com.apicatalog.jsonld.document.Document;
import com.apicatalog.jsonld.document.JsonDocument;
import com.apicatalog.jsonld.document.RdfDocument;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
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
import net.minidev.json.JSONObject;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class HttpServerVerticle extends AbstractVerticle {

    String jsonPath = "/api/JsonLD/DetailInLD";
    String host = "localhost";
    int port = 9090;

    public enum Type {

        accommodation;

    }

    @Override
    public void start() throws Exception {

        System.out.println("Vert.x instance: " + vertx);

        Router router = Router.router(vertx);

        HttpServerOptions options = new HttpServerOptions()
                .setPort(port)
                .setHost(host);

        io.vertx.core.http.HttpServer server = vertx.createHttpServer(options);

        // Test route
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

            // Test
            System.out.println( "URI: " + request.uri() );
            System.out.println( "METHOD: " + request.method() );
            System.out.println( "VERSION: " + request.version() );
            System.out.println( "PATH: " + request.path() );
            System.out.println( "QUERY: " + request.query() );

            // Get query parameters
            String type = request.getParam("type");
            String id = request.getParam("Id");

            String endpoint = request.getParam("endpoint") ;
            if (endpoint == null || endpoint.isEmpty()) {
                endpoint = "sparql.opendatahub.bz.it";
            }
            String language = request.getParam("language") ;
            if (language == null || language.isEmpty()) {
                language = "en";
            }

            String showid = request.getParam("showid");
            if (showid == null || showid.isEmpty()) {
                showid = "true";
            }

            HttpServerResponse response = routingContext.response();
            if(type == null || id == null ){
                response.setStatusCode(400).end("The request" + request.uri() + " is invalid.");
            }

            WebClient client = WebClient.create(vertx);

            try {
                String sparqlQuery = getConstructSparqlQuery(type, language, id);

                String frame = getFrame(type);

                boolean showIdBoolean = Boolean.parseBoolean(showid);

//                Buffer buffer = Buffer.buffer("query="+sparqlQuery);
                // Send a GET request
                client
                        .get(8080, endpoint, "/sparql")
                        .addQueryParam("query", sparqlQuery)
                        .putHeader("Accept", "application/ld+json")
                        .send( ar -> {
                            if (ar.succeeded()) {
                                // Obtain response
                                HttpResponse<Buffer> res = ar.result();
                                String result = res.bodyAsString();

//                                result ="[{\"@id\":\"http://noi.example.org/ontology/odh#data/accommodation/70043B17DAE33F1EFCDA24D4BB4C1F72\",\"@type\":[\"http://schema.org/LodgingBusiness\"],\"http://schema.org/name\":[{\"@value\":\"Ütia Gardenacia\"}],\"http://schema.org/address\":[{\"@id\":\"http://noi.example.org/ontology/odh#data/address/accommodation/70043B17DAE33F1EFCDA24D4BB4C1F72\"}]},{\"@id\":\"http://noi.example.org/ontology/odh#data/address/accommodation/70043B17DAE33F1EFCDA24D4BB4C1F72\",\"@type\":[\"http://schema.org/PostalAddress\"]},{\"@id\":\"http://schema.org/LodgingBusiness\"},{\"@id\":\"http://schema.org/PostalAddress\"}]";

                                System.out.println("Received response with status code " + res.statusCode());
                                System.out.println(result);

                                InputStream streamJsonLd =  new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8));

                                InputStream streamFrame =  new ByteArrayInputStream(frame.getBytes(StandardCharsets.UTF_8));

                                String responseJson = "";
                                try {
                                    JsonDocument jsonldDoc = JsonDocument.of(streamJsonLd);
                                    JsonDocument frameDoc = JsonDocument.of(streamFrame);
                                    JsonObject framed = JsonLd.frame(jsonldDoc, frameDoc).get();

                                    if (!showIdBoolean) {
                                        DocumentContext jsonContext = JsonPath.parse(framed.toString());
                                        jsonContext.delete("$..id");
                                        responseJson = jsonContext.jsonString();
                                    }

                                    response.putHeader("content-type", "application/ld+json");

                                    response.end(responseJson);
                                } catch (JsonLdError jsonLdError) {
                                    jsonLdError.printStackTrace();
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

    private String getFrame(String type) throws Exception {


        switch (Type.valueOf(type)) {
            case accommodation:
                return "{\n" +
                        "  \"@context\": \"http://schema.org/\",\n" +
                        "  \"@type\": [\"LodgingBusiness\", \"Hotel\"],\n" +
                        "  \"address\": {\n" +
                        "    \"@type\": \"PostalAddress\"\n" +
                        "  }\n" +
                        "}";

            default:
                throw  new Exception("Unsupported request");
        }

    }

    private String getConstructSparqlQuery(String type, String language, String id) throws Exception {

        switch (Type.valueOf(type)) {
            case accommodation:
                return getAccommodationConstructQuery(language, id);

            default:
                throw new Exception("Unsupported request");
        }

    }

    //TODO check id and language for sql injection
    private String getAccommodationConstructQuery(String language, String id) {

        String uri = "<http://noi.example.org/ontology/odh#data/accommodation/"+id+">";
        return "PREFIX schema: <http://schema.org/>\n" +
                "CONSTRUCT {\n" +
                "  ?h a ?cl ; schema:name ?nameStr ; schema:description ?desc ; schema:url ?url ; \n" +
                "    schema:address ?a .\n" +
                "   ?a a schema:PostalAddress ; schema:name ?aName ; schema:alternateName ?aAltName \n" +
                "\n" +
                "}\n" +
                "WHERE {\n" +
                "  ?h a schema:LodgingBusiness ; schema:name ?name ; schema:address ?a .\n" +
                "  ?a a schema:PostalAddress .\n" +
                "  FILTER (?h = "+uri+")"+
                "  \n" +
                "  # TODO: map it\n" +
                "  OPTIONAL {\n" +
                "    ?h schema:description ?desc . \n" +
                "    FILTER (lang(?name) = lang(?desc))\n" +
                "  }\n" +
                "  \n" +
                "  # TODO: map it\n" +
                "  OPTIONAL {\n" +
                "    ?h schema:url ?url .\n" +
                "  }\n" +
                "  \n" +
                "  # TODO: map it\n" +
                "  OPTIONAL {\n" +
                "    ?a schema:name ?aName .\n" +
                "    FILTER (lang(?name) = lang(?aName))\n" +
                "  }\n" +
                "\n" +
                "  # TODO: map it\n" +
                "  OPTIONAL {\n" +
                "    ?a schema:alternateName ?aAltName .\n" +
                "    FILTER (lang(?name) = lang(?aAltName))\n" +
                "  }\n" +
                "  \n" +
                "  OPTIONAL {\n" +
                "    ?h a schema:Hotel .\n" +
                "    BIND (schema:Hotel AS ?c)\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?h a schema:Campground .\n" +
                "    BIND (schema:Campground AS ?c)\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?h a schema:BedAndBreakfast .\n" +
                "    BIND (schema:BedAndBreakfast AS ?c)\n" +
                "  }\n" +
                "  OPTIONAL {\n" +
                "    ?h a schema:Hostel .\n" +
                "    BIND (schema:Hostel AS ?c)\n" +
                "  }\n" +
                "  BIND (COALESCE(?c, schema:LodgingBusiness) AS ?cl)\n" +
                "  \n" +
                "  # Hiding language tags\n" +
                "  BIND(str(?name) AS ?nameStr)\n" +
                "  \n" +
                "  FILTER (lang(?name) = COALESCE('"+language+"', 'en'))\n" +
                "}\n" ;
    }
}
