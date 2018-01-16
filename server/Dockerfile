FROM towerhawk/alpine-java-onbuild:8-1.1.0

ARG JAR=*.jar

ADD target/$JAR $APP_HOME/

USER $USER:$GROUP
