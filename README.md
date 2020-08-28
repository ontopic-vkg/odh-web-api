# JSON-LD Web Api for the Open Data Hub (ODH).

This project is a JSON-LD web api for for the Open Data Hub (ODH) 
displaying data in JSON-LD format from a SPARQL endpoint using schema.org standard.


## Table of contents

- [Usage](#usage)
- [Parameters](#parameters)
- [Information](#information)

## Usage


### Prerequisites

#### Docker environment

For the project a Docker environment is already prepared and ready to use with all necessary prerequisites.

#### Installation

Install [Docker](https://docs.docker.com/install/) locally on your machine.

### Build and run docker 

Before start working you have to build the Docker image:

```
docker build -t ontopic-web-api/json-ld .
```

The you can run the image:

```
docker run -t -i -p 9090:9090 ontopic-web-api/json-ld
```

### Local deployment

1. Build and run the Docker 

2. Test the web api

* Now we can open the link <http://localhost:9090/api/JsonLD/DetailInLD> 
in the browser and test the web api adding the mandatory parameters type and Id.

* All the accepted parameters can be seen in [the dedicated section](#parameters)

Example: http://localhost:9090/api/JsonLD/DetailInLD?type=accommodation&Id=70043B17DAE33F1EFCDA24D4BB4C1F72

## Parameters

#### type 
Data Type to transform currently available only 'accommodation', required
Example: type=accommodation

#### Id
ID of the data to transform, required
Example Id=70043B17DAE33F1EFCDA24D4BB4C1F72

#### endpoint
Absolute url of the SPARQL endpoint 
Default value : https://sparql.opendatahub.bz.it/sparql
Example endpoint=http://host.docker.internal:8080/sparql

#### language
Output Language
Default value : en
Example language=de

#### idtoshow
Url to show on Json LD @id, not provided Id of ODH api call is taken
Example idtoshow=http://noi.example.org/23


#### imageurltoshow
image url to show on Json LD @image, not provided image url of data is taken
Example imageurltoshow=http://noi.example.org/images/image23.jpg

#### showid
Show the @id property in Json LD
Default value : true
Example showid=false


## Information

### Support

For support, please contact [info@opendatahub.bz.it](mailto:info@opendatahub.bz.it).

### Contributing

If you'd like to contribute, please follow the following instructions:

- Fork the repository.

- Checkout a topic branch from the `development` branch.

- Make sure the tests are passing.

- Create a pull request against the `development` branch.

A more detailed description can be found here: [https://github.com/noi-techpark/documentation/blob/master/contributors.md](https://github.com/noi-techpark/documentation/blob/master/contributors.md).

### Documentation

More documentation can be found at [https://opendatahub.readthedocs.io/en/latest/index.html](https://opendatahub.readthedocs.io/en/latest/index.html).

