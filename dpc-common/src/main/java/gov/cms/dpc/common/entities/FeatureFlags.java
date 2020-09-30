package gov.cms.dpc.common.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Optional;

public class FeatureFlags {
    private ObjectNode featuresRootNode;

    public FeatureFlags(){
        featuresRootNode = JsonNodeFactory.instance.objectNode();
    }

    public Optional<JsonNode> getFeaturesRootNode() {
        return Optional.ofNullable(featuresRootNode);
    }

    public void setFeaturesRootNode(ObjectNode featuresRootNode) {
        this.featuresRootNode = featuresRootNode;
    }

    public void setFeature(String featureCode, String value){
        featuresRootNode.set(featureCode,JsonNodeFactory.instance.textNode(value));
    }

    public void setFeature(String featureCode, double value){
        featuresRootNode.set(featureCode,JsonNodeFactory.instance.numberNode(value));
    }

    public void setFeature(String featureCode, int value){
        featuresRootNode.set(featureCode,JsonNodeFactory.instance.numberNode(value));
    }

    public void setFeature(String featureCode, long value){
        featuresRootNode.set(featureCode,JsonNodeFactory.instance.numberNode(value));
    }

    public void setFeature(String featureCode, boolean value){
        featuresRootNode.set(featureCode,JsonNodeFactory.instance.booleanNode(value));
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
        return Optional.ofNullable(featuresRootNode.get(code));
    }
}
