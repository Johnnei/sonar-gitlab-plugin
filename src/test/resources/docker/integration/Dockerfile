FROM centos:latest
MAINTAINER Johnnei

ARG MAVEN_VERSION=3.3.9
ARG GIT_VERSION=2.10.2

RUN yum update -y

###
# Install sofware
###

# Install Java
RUN mkdir -p /tmp/dist
RUN curl -L -C - -b "oraclelicense=accept-securebackup-cookie" -o /tmp/dist/java.rpm http://download.oracle.com/otn-pub/java/jdk/8u111-b14/jdk-8u111-linux-x64.rpm
RUN rpm -ivh /tmp/dist/java.rpm
ENV JAVA_HOME="/usr/java/jdk1.8.0_111"

# Install Maven 3.3.9 (yum only offers 3.0)
RUN mkdir -p /usr/share/maven /usr/share/maven/ref \
  && curl -fsSL http://apache.osuosl.org/maven/maven-3/$MAVEN_VERSION/binaries/apache-maven-$MAVEN_VERSION-bin.tar.gz \
    | tar -xzC /usr/share/maven \
  && mv /usr/share/maven/apache-maven-$MAVEN_VERSION/* /usr/share/maven \
  && ln -s /usr/share/maven/bin/mvn /usr/bin/mvn \
  && chmod +x /usr/share/maven/bin/mvn

# Install Docker from RPM
COPY docker-repo.txt /dist/tmp/docker-repo.txt
RUN cat /dist/tmp/docker-repo.txt >> /etc/yum.repos.d/docker.repo
RUN yum install -y docker

# Install git (My god Red Hat, Why do you supply stone age git?)
RUN yum groupinstall -y "Development Tools" && yum install -y curl-devel expat-devel gettext-devel openssl-devel perl-CPAN perl-devel zlib-devel
RUN mkdir -p /usr/share/git \
  && curl -fsSL https://github.com/git/git/archive/v$GIT_VERSION.tar.gz \
    | tar -xzC /usr/share/git/ \
  && mv /usr/share/git/git-$GIT_VERSION/* /usr/share/git

RUN pushd /usr/share/git && make configure && ./configure --prefix=/usr/local && make install && popd

RUN rm -r /tmp/dist

###
# Configure installed software
###

# Configure Maven
COPY settings.xml /usr/share/maven/conf/settings.xml
RUN echo "" >> /usr/share/maven/conf/logging/simplelogger.properties
RUN echo "org.slf4j.simpleLogger.log.org.apache=info" >> /usr/share/maven/conf/logging/simplelogger.properties
RUN echo "org.slf4j.simpleLogger.log.org.codehaus=info" >> /usr/share/maven/conf/logging/simplelogger.properties

# Link to DIND which should be linked to this container.
ENV DOCKER_HOST="tcp://fast-dind:2375"

CMD "mvn"