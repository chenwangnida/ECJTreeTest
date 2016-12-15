package wsc.graph;

public class ServicePrecondition {
	private String precondition;
	boolean isSatified;

	public ServicePrecondition(String precondition, boolean isSatified) {
		super();
		this.precondition = precondition;
		this.isSatified = isSatified;
	}

	public String getPrecondition() {
		return precondition;
	}

	public void setPrecondition(String precondition) {
		this.precondition = precondition;
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
		result = prime * result + ((precondition == null) ? 0 : precondition.hashCode());
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
		ServicePrecondition other = (ServicePrecondition) obj;
		if (isSatified != other.isSatified)
			return false;
		if (precondition == null) {
			if (other.precondition != null)
				return false;
		} else if (!precondition.equals(other.precondition))
			return false;
		return true;
	}

	
}
