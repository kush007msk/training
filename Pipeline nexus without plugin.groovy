pipeline{
 agent {label 'master'}
 environment {
      sonar_url = 'http://172.31.40.185:9000'
      sonar_username = 'admin'
      sonar_password = 'admin'
      NEXUS_VERSION = "nexus3"
      NEXUS_PROTOCOL = "http"
      NEXUS_URL = "172.31.50.152:8081"
      NEXUS_REPOSITORY = "release"
      NEXUS_CREDENTIAL_ID = "nexus-cred"
      }
 options {
        buildDiscarder(logRotator(numToKeepStr: '5', artifactNumToKeepStr: '5'))
        ansiColor('xterm')
    }
    
     
  tools{
    jdk 'Java'
    maven 'Maven'
    }
    
    
    parameters { 
         string(name: 'Repo', defaultValue: 'helloworld-project-1', description: 'please choose repo to build')
       choice(name: 'Branch', choices: ['main', 'develop', 'feature'], description: 'please choose branch to build') 
    }
stages{
 stage ('Git clone'){
    steps{
     git branch: '${Branch}',
     url: 'https://github.com/kush007msk/${Repo}'
   }
  }
  stage ('Maven Build'){
    steps{
     sh 'mvn clean package'
   }
  }
  stage ('Sonarqube Analysis'){
           steps {
           withSonarQubeEnv('sonarqube') {
           sh '''
           mvn clean package org.jacoco:jacoco-maven-plugin:prepare-agent install -Dmaven.test.failure.ignore=false
           mvn -e -B sonar:sonar -Dsonar.java.source=1.8 -Dsonar.host.url="${sonar_url}" -Dsonar.login="${sonar_username}" -Dsonar.password="${sonar_password}" -Dsonar.sourceEncoding=UTF-8
           '''
           }
         }
      }
       stage("Publish to Nexus Repository Manager") {
            steps {
                script {
                    pom = readMavenPom file: "pom.xml";
                    filesByGlob = findFiles(glob: "target/*.${pom.packaging}");
                    echo "${filesByGlob[0].name} ${filesByGlob[0].path} ${filesByGlob[0].directory} ${filesByGlob[0].length} ${filesByGlob[0].lastModified}"
                    artifactPath = filesByGlob[0].path;
                    artifactExists = fileExists artifactPath;
                    if(artifactExists) {
                        echo "*** File: ${artifactPath}, group: ${pom.groupId}, packaging: ${pom.packaging}, version ${pom.version}";
                        nexusArtifactUploader(
                            nexusVersion: 'nexus3',
                            protocol: 'http',
                            nexusUrl: '172.31.50.152:8081',
                            groupId: 'com.efsavage',
                            version: '1.0.0',
                            repository: 'release',
                            credentialsId: 'nexus-cred',
                            artifacts: [
                                [artifactId: 'hello-world-war',
                                classifier: '',
                                file: artifactPath,
                                type: pom.packaging],
                                [artifactId: pom.artifactId,
                                classifier: '',
                                file: "target/hello-world-war-1.0.0.war",
                                type: "war"]
                            ]
                        );
                    } else {
                        error "*** File: ${artifactPath}, could not be found";
                    }
                }
            }
        }
 }
}