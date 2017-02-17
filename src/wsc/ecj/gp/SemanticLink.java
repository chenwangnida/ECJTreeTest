package wsc.ecj.gp;

public class SemanticLink {
	String SourceService;
	String TargetService;
	double avgmt;
	double avgsdt;

	public String getSourceService() {
		return SourceService;
	}

	public void setSourceService(String sourceService) {
		SourceService = sourceService;
	}

	public String getTargetService() {
		return TargetService;
	}

	public void setTargetService(String targetService) {
		TargetService = targetService;
	}

	public double getAvgmt() {
		return avgmt;
	}

	public void setAvgmt(double avgmt) {
		this.avgmt = avgmt;
	}

	public double getAvgsdt() {
		return avgsdt;
	}

	public void setAvgsdt(double avgsdt) {
		this.avgsdt = avgsdt;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((SourceService == null) ? 0 : SourceService.hashCode());
		result = prime * result + ((TargetService == null) ? 0 : TargetService.hashCode());
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
		SemanticLink other = (SemanticLink) obj;
		if (SourceService == null) {
			if (other.SourceService != null)
				return false;
		} else if (!SourceService.equals(other.SourceService))
			return false;
		if (TargetService == null) {
			if (other.TargetService != null)
				return false;
		} else if (!TargetService.equals(other.TargetService))
			return false;
		return true;
	}

}
