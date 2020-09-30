package gov.cms.dpc.common.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public class FeatureFlags {
    private ObjectNode featuresRootNode;

    public FeatureFlags(){}

    public ObjectNode getFeaturesRootNode() {
        return featuresRootNode;
    }

    public void setFeaturesRootNode(ObjectNode featuresRootNode) {
        this.featuresRootNode = featuresRootNode;
    }

    public void setFeature(String featureCode, String value){
        setFeature(featureCode,JsonNodeFactory.instance.textNode(value));
    }

    public void setFeature(String featureCode, double value){
        setFeature(featureCode,JsonNodeFactory.instance.numberNode(value));
    }

    public void setFeature(String featureCode, int value){
        setFeature(featureCode,JsonNodeFactory.instance.numberNode(value));
    }

    public void setFeature(String featureCode, long value){
        setFeature(featureCode,JsonNodeFactory.instance.numberNode(value));
    }

    public void setFeature(String featureCode, boolean value){
        setFeature(featureCode, JsonNodeFactory.instance.booleanNode(value));
    }

    public void setFeature(String featureCode, JsonNode node){
        if(featuresRootNode == null){
            featuresRootNode = JsonNodeFactory.instance.objectNode();
        }
        featuresRootNode.set(featureCode,node);
    }

    public Optional<String> getStringFeature(String featureCode){
            return getValueNode(featureCode).map(f->f.asText());
    }

    public Optional<Double> getDoubleFeature(String featureCode){
        return getValueNode(featureCode).map(f->f.asDouble());
    }

    public Optional<Integer> getIntegerFeature(String featureCode){
        return getValueNode(featureCode).map(f->f.asInt());
    }

    public Optional<Long> getLongFeature(String featureCode){
        return getValueNode(featureCode).map(f->f.asLong());
    }

    public Optional<Boolean> getBooleanFeature(String featureCode){
        return getValueNode(featureCode).map(f->f.asBoolean());
    }

    public void removeFeature(String featureCode){
        featuresRootNode.remove(featureCode);
    }

    private Optional<JsonNode> getValueNode(String code){
        if(featuresRootNode == null){
            return Optional.empty();
        }
        return Optional.ofNullable(featuresRootNode.get(code));
    }
}
