
def label = "packager-${UUID.randomUUID().toString()}"

podTemplate(
    label: label,
    containers: [
        containerTemplate(
            name: 'maven',
            image: 'maven:3-jdk-11',
            ttyEnabled: true,
            command: 'cat',
            resourceRequestMemory: '1Gi',
            resourceLimitMemory: '2Gi',
        )
    ],
    volumes: [
        configMapVolume(mountPath: '/home/jenkins/.ssh', configMapName: 'known-hosts', readOnly: true)
    ]
) {

    node(label) {

        container('maven') {
            stage('Checkout') {
                git 'https://github.com/eclipse/packager.git'
            }
            stage('Build') {
                sh 'mvn -B package'
            }
            stage('Test') {
                sh 'mvn -B test'
            }
        }

    }

}
