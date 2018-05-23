package io.objectbox.external;
import java.util.Map;

/**
 * @author Juan Ramos - ptjuanramos
 */
public class EndPoints {
	private boolean isEnable;
	private String topHierarchical;
	private Map<QueryType, String> endPoints;
	
	public EndPoints(EndPointsBuilder endpointsBuilder) {
		this.isEnable = endpointsBuilder.isEnable;
		this.topHierarchical = endpointsBuilder.topHierarchical;
		this.endPoints = endpointsBuilder.endPoints;
	}
	
	public Map<QueryType, String> getEndPoints() {
		return endPoints;
	}
	
	public String getTopHierarchical() {
		return topHierarchical;
	}
	
	public boolean isEnable() {
		return isEnable;
	}
	
	/**
	 * 
	 * @author juan_
	 *
	 */
	public static class EndPointsBuilder {
		private boolean isEnable;
		private String topHierarchical;
		private Map<QueryType, String> endPoints;
		
		/**
		 * 
		 * @param isEnable
		 * @return
		 */
		public EndPointsBuilder enable(boolean isEnable) {
			this.isEnable = isEnable;
			return this;
		}
		
		/**
		 * 
		 * @param uri
		 * @return
		 */
		public EndPointsBuilder main(String topHierarchical) {
			this.topHierarchical = topHierarchical;
			return this;
		}
		
		/**
		 * 
		 * @param endPoints
		 * @return
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
