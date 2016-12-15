package wsc.graph;

public class ServicePostcondition {
	private String postcondition;
	boolean isSatified;

	public ServicePostcondition(String postcondition, boolean isSatified) {
		super();
		this.postcondition = postcondition;
		this.isSatified = isSatified;
	}

	public String getPostcondition() {
		return postcondition;
	}

	public void setPostcondition(String postcondition) {
		this.postcondition = postcondition;
	}

	public boolean isSatified() {
		return isSatified;
	}

	public void setSatified(boolean isSatified) {
		this.isSatified = isSatified;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (isSatified ? 1231 : 1237);
		result = prime * result + ((postcondition == null) ? 0 : postcondition.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServicePostcondition other = (ServicePostcondition) obj;
		if (isSatified != other.isSatified)
			return false;
		if (postcondition == null) {
			if (other.postcondition != null)
				return false;
		} else if (!postcondition.equals(other.postcondition))
			return false;
		return true;
	}

}
