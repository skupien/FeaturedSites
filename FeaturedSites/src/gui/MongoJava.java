package gui;

import com.mongodb.*;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.ObjectId;

import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;

import java.lang.Math;
import java.util.*;

public class MongoJava {
    MongoDatabase mongodb; //used for aggregation

    public MongoJava() {
        MongoClient mongoClient = new MongoClient();
        mongodb = mongoClient.getDatabase("FeaturedSitesProject");

        Dictionary<Integer, String> education = new Hashtable<>();
        education.put(1, "None");
        education.put(2, "Primary");
        education.put(3, "Secondary");
        education.put(4, "Bachelor");
        education.put(5, "Master");
        education.put(6, "PhD");
        education.put(7, "Professor");
    }

    static double mean(List<Double> numbers) {
        double numerator = 0;
        double denominator = 0;
        for(Double num : numbers) {
            numerator += num;
            denominator++;
        }
        return numerator/denominator;
    }

    List<ObjectId> getRandomUserIds(int quantity) {
        AggregateIterable<Document> output = mongodb.getCollection("user").aggregate(Arrays.asList(
                sample(quantity),
                project(new Document("_id", "$_id"))
        ));

        List<ObjectId> randFriends = new ArrayList<>();
        for (Document dbObject : output) {
            randFriends.add( dbObject.getObjectId("_id"));
        }
        return randFriends;
    }

    private double axisMean(Document user, Document site) {
        double pol = Math.abs(site.getDouble("politics") - user.getDouble("politics"));
        double ec = Math.abs(site.getDouble("economics") - user.getDouble("economics"));
        double rel = Math.abs(site.getDouble("religiousness") - user.getDouble("religiousness"));
        double emp = Math.abs(site.getDouble("empathy") - user.getDouble("empathy"));
        return MongoJava.mean(List.of(pol,ec,rel,emp));
    }

    Map<ObjectId, Map<ObjectId, Double>> findFeaturedSites(double radius) {
        FindIterable<Document> sites = mongodb.getCollection("site").find();
        Map<ObjectId, Map<ObjectId, Double>> fullData = new HashMap<>();

        for(Document site : sites) {
            Map<ObjectId, Double> potentialUsers = new HashMap<>();

            //here checks every potential user:
            FindIterable<Document> users = mongodb.getCollection("user").find();
            for(Document user : users) {
                double mean = axisMean(user,site);
                if (mean <= radius) {
                    mean = Math.round(mean*100.0)/100.0;
                    potentialUsers.put(user.getObjectId("_id"), mean);
                }
            }
            fullData.put(site.getObjectId("_id"), potentialUsers);
        }
        return fullData;
    }

    private AggregateIterable<Document> getFriendsOutput(List<ObjectId> friends) {
        return mongodb.getCollection("user").aggregate(Arrays.asList(
                match(in("_id", friends)),
                new Document("$group",
                        (new Document("_id", null)
                                .append("politics", new Document("$avg", "$politics"))
                                .append("economics", new Document("$avg", "$economics"))
                                .append("religiousness", new Document("$avg", "$religiousness"))
                                .append("empathy", new Document("$avg", "$empathy"))
                                .append("pVegan", new Document("$sum", "$preferences.Vegan"))
                                .append("pAesthetics", new Document("$sum", "$preferences.Aesthetics"))
                                .append("pEcology", new Document("$sum", "$preferences.Ecology"))
                                .append("pSynthwave", new Document("$sum", "$preferences.Synthwave"))
                                .append("pBikes", new Document("$sum", "$preferences.Bikes"))
                                .append("pArt", new Document("$sum", "$preferences.Art"))
                                .append("pAnime", new Document("$sum", "$preferences.Anime"))))));
    }

    void addUserWithFriends(String name, String surname, double age, String education, List<ObjectId> friendsID) {
        Document axis;
        if (Objects.isNull(friendsID) || friendsID.isEmpty()) {
            axis = new Document("politics", 0.0)
                    .append("economics", 0.0)
                    .append("religiousness", 0.0)
                    .append("empathy", 0.0)
                    .append("pVegan", 0.0)
                    .append("pAesthetics", 0.0)
                    .append("pEcology", 0.0)
                    .append("pSynthwave", 0.0)
                    .append("pBikes", 0.0)
                    .append("pArt", 0.0)
                    .append("pAnime", 0.0);
        }
        else {
            AggregateIterable<Document> output = getFriendsOutput(friendsID);
            axis = output.first();
        }

        Document tmp = new Document("name", name)
                .append("surname", surname)
                .append("age", age)
                .append("education", education)
                .append("politics",      Math.round(axis.getDouble("politics")*100)/100.0)
                .append("economics",     Math.round(axis.getDouble("economics")*100)/100.0)
                .append("religiousness", Math.round(axis.getDouble("religiousness")*100)/100.0)
                .append("empathy",       Math.round(axis.getDouble("empathy")*100)/100.0)
                .append("friends",       friendsID)
                .append("preferences",   new Document("Vegan", Math.signum(axis.getDouble("pVegan")))
                        .append("Aesthetics", Math.signum(axis.getDouble("pAesthetics")))
                        .append("Ecology",    Math.signum(axis.getDouble("pEcology")))
                        .append("Synthwave",  Math.signum(axis.getDouble("pSynthwave")))
                        .append("Bikes",      Math.signum(axis.getDouble("pBikes")))
                        .append("Art",        Math.signum(axis.getDouble("pArt")))
                        .append("Anime",      Math.signum(axis.getDouble("pAnime")))
                );
        mongodb.getCollection("user").insertOne(tmp);
        System.out.println("Dodawanie: "  + name + " " + surname);
        return;
    }

    Map<ObjectId, Double> findFeaturedSitesForUser(ObjectId id, double radius) {
        System.out.println("Maksymalna rozbieznosc: " + radius);
        Map<ObjectId, Double> potentialSites = new HashMap<>();
        System.out.println("Proponowane strony:");
        boolean atLeastOne = false;
        FindIterable<Document> users = mongodb.getCollection("user").find(eq("_id", id));
        Document user = users.first();
        FindIterable<Document> sites = mongodb.getCollection("site").find();
        for(Document site : sites) {
            double pol = Math.abs(site.getDouble("politics") - user.getDouble("politics"));
            double ec = Math.abs(site.getDouble("economics") - user.getDouble("economics"));
            double rel = Math.abs(site.getDouble("religiousness") - user.getDouble("religiousness"));
            double emp = Math.abs(site.getDouble("empathy") - user.getDouble("empathy"));
            double mean = MongoJava.mean(List.of(pol,ec,rel,emp));
            if (mean <= radius) {
                System.out.println(site.getString("name") + " (" + user.getObjectId("_id") + ")" + " - bliskosc przypasowania: " + mean);
                atLeastOne = true;
                potentialSites.put(site.getObjectId("_id"), mean);
            }
        }
        if(!atLeastOne) System.out.println("BRAK PROPONOWNAYCH STRON");
        return potentialSites;
    }

    private void calculateAverage(ObjectId first) {
        double pol, ec, rel, emp;
        double pol2, ec2, rel2, emp2;
        Document doc1 = mongodb.getCollection("user").find(eq("_id", first)).first();
        pol =  doc1.getDouble("politics");
        ec =  doc1.getDouble("economics");
        rel =  doc1.getDouble("religiousness");
        emp =  doc1.getDouble("empathy");

        List<ObjectId> friends = (List<ObjectId>) doc1.get("friends");
        AggregateIterable<Document> output = getFriendsOutput(friends);
        Document doc2 = output.first();
        pol2 =  Math.round(doc2.getDouble("politics")*100)/100.0;
        ec2 =  Math.round(doc2.getDouble("economics")*100)/100.0;
        rel2 =  Math.round(doc2.getDouble("religiousness")*100)/100.0;
        emp2 =  Math.round(doc2.getDouble("empathy")*100)/100.0;
        System.out.print("Uzytkownik o id " + first + " - ");
        System.out.println("zmiany na osiach:");
        System.out.println("Politycznych:  " +  Math.round((pol2 - pol)*100)/100.0);
        System.out.println("Ekonomicznych: " +  Math.round((ec2 - ec)*100)/100.0);
        System.out.println("Religijnych:   " +  Math.round((rel2 - rel)*100)/100.0);
        System.out.println("Spolecznych:   " +  Math.round((emp2 - emp)*100)/100.0);

        mongodb.getCollection("user").updateMany(new Document("_id", first),
        new Document("$set",
                    new Document("politics",           pol2)
                    .append("economics",               ec2)
                    .append("religiousness",           rel2)
                    .append("empathy",                 emp2)
                    .append("preferences.Vegan",       Math.signum(doc2.getDouble("pVegan")))
                    .append("preferences.Aesthetics",  Math.signum(doc2.getDouble("pAesthetics")))
                    .append("preferences.Ecology",     Math.signum(doc2.getDouble("pEcology")))
                    .append("preferences.Synthwave",   Math.signum(doc2.getDouble("pSynthwave")))
                    .append("preferences.Bikes",       Math.signum(doc2.getDouble("pBikes")))
                    .append("preferences.Art",         Math.signum(doc2.getDouble("pArt")))
                    .append("preferences.Anime",       Math.signum(doc2.getDouble("pAnime")))
        ));
    }

    private void befriend(ObjectId first, ObjectId second) {
        System.out.println("Befriending: " + first + " with " + second);

        mongodb.getCollection("user").updateOne(eq("_id", first), Updates.addToSet("friends", second));
        mongodb.getCollection("user").updateOne(eq("_id", second), Updates.addToSet("friends", first));

        calculateAverage(first);
        calculateAverage(second);
    }
}