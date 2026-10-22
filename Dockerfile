FROM eclipse-temurin:17-jdk-jammy as builder
WORKDIR /opt/app
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
COPY ./src ./src
RUN ./mvnw clean install

FROM eclipse-temurin:17-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends ca-certificates \
 && sed -i -E 's#^(mozilla/emSign_(ECC_Root_CA_-_C3|Root_CA_-_C1|ECC_Root_CA_-_G3)\.crt)#!\1#' /etc/ca-certificates.conf \
 && update-ca-certificates --fresh && rm -rf /var/lib/apt/lists/*
RUN for a in "cn_emsign_ecc_root_ca__c3,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" \
             "cn_emsign_root_ca__c1,o_emudhra_inc,ou_emsign_pki,c_us [jdk]" \
             "cn_emsign_ecc_root_ca__g3,o_emudhra_technologies_limited,ou_emsign_pki,c_in [jdk]"; do \
      keytool -delete -cacerts -storepass changeit -alias "$a"; done
WORKDIR /opt/app
EXPOSE 8080
COPY --from=builder /opt/app/target/*.jar /opt/app/*.jar
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar" ]