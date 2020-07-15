# Dataswift-challenge

Start the server with 

```
sbt run
```

Every 10 seconds it will start synchronizing with github and store in the database

```
curl localhost:8080/challenge
```

Will return the current stored collection.
