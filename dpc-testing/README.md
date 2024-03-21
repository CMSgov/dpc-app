# dpc-testing

## Table of Contents
* [AbstractDAOTest](#abstractdaotest)
* [NoExitSecurityManager](#noexitsecuritymanager)

<a id="AbstractDAOTest"></a>
### AbstractDAOTest
This class is used to write tests for DAO objects.  It provides a ```DAOTestExtension``` configured to use Postgres in a TestContainer for your tests.  It takes care of the lifecycle of the container, manages DB migrations and ensures that you have a fresh DB for each of your tests.

For an example of how to use the class, see [IpAddressDAOUnitTest.java](https://github.com/CMSgov/dpc-app/blob/master/dpc-api/src/test/java/gov/cms/dpc/api/jdbi/IpAddressDAOTest.java)

For info on TestContainers, see: [https://testcontainers.com](https://testcontainers.com)

For info on DAOTestExtensions, see: [Drop Wizard Testing](https://www.dropwizard.io/en/latest/manual/testing.html#testing-database-interactions)


### NoExitSecurityManager
This class is used if you need to write tests for code that calls ```System.exit()```.  Normally, if the code you're calling calls ```System.exit()``` everything shuts down and your tests stop running.  If you replace the security manager with this class, a ```SystemExitException``` will be thrown instead that your tests can catch.

Use it like this:
```java
SecurityManager originalSecurityManager = System.getSecurityManager();
System.setSecurityManager(new NoExitSecurityManager());

//<CODE THAT CALLS System.exit() HERE>

System.setSecurityManager(originalSecurityManager);
```

For an example, see: [KeyDeleteUnitTest.java](https://github.com/CMSgov/dpc-app/blob/c495f9b9ad035291b82b641b031c21e627b08424/dpc-api/src/test/java/gov/cms/dpc/api/cli/keys/KeyDeleteUnitTest.java#L108)