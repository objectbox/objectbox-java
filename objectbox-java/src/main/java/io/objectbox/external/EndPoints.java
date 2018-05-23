package io.objectbox.external;
import java.util.Map;

/**
 * Class that represents the set of end points that are used for HTTP requests
 * @author Juan Ramos - ptjuanramos
 */
public class EndPoints {
	private boolean isEnable; //this variable isn't used actually, but we are going to when we need to sync DBs
	private String topHierarchical;
	private Map<QueryType, String> endPoints;
	
	public EndPoints(EndPointsBuilder endpointsBuilder) {
		this.isEnable = endpointsBuilder.isEnable;
		this.topHierarchical = endpointsBuilder.topHierarchical;
		this.endPoints = endpointsBuilder.endPoints;
	}
	
	/**
	 * Gets a {@link Map} object that were add by user instances and {@link EndPointsBuilder}.
	 * <br>This {@link Map} represents a {@link QueryType} => End point String association
	 * @return {@link Map}
	 */
	public Map<QueryType, String> getEndPoints() {
		return endPoints;
	}
	
	/**
	 * Top hierarchical is the base URI prefix that is used to make HTTP requests
	 * @return String that represents the base URI
	 */
	public String getTopHierarchical() {
		return topHierarchical;
	}
	
	/**
	 * Check if this feature is enable or not.
	 * <br>TODO - trying to figuring out if this is really necessary.
	 * @return Boolean
	 */
	public boolean isEnable() {
		return isEnable;
	}
	
	/**
	 * Following the Builder design pattern this {@link EndPoints} inner class builds
	 * 	a {@link EndPoints} instance with all properties values
	 */
	public static class EndPointsBuilder {
		private boolean isEnable;
		private String topHierarchical;
		private Map<QueryType, String> endPoints;
		
		/**
		 * Set {@link EndPoints} enable property true or false
		 * @return {@link EndPoints} instance
		 */
		public EndPointsBuilder enable(boolean isEnable) {
			this.isEnable = isEnable;
			return this;
		}
		
		/**
		 * Sets the base URI
		 * @param uri String object that represents the base URI
		 * @return {@link EndPoints} instance
		 */
		public EndPointsBuilder main(String topHierarchical) {
			this.topHierarchical = topHierarchical;
			return this;
		}
		
		/**
		 * The most important feature... This method sets a {@link Map} instance to build a {@link EndPoints} instance
		 * @param endPoints {@link Map} instance
		 * @return {@link EndPoints} instance
		 */
		public EndPointsBuilder setEndpoints(Map<QueryType, String> endPoints) {
			this.endPoints = endPoints;
			return this;
		}
		
		public EndPoints build() {
			return new EndPoints(this);
		}
	}
}
