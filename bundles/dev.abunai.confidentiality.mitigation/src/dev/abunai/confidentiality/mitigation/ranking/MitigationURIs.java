package dev.abunai.confidentiality.mitigation.ranking;

import org.eclipse.emf.common.util.URI;

public record MitigationURIs(URI modelUncertaintyURI, URI mitigationUncertaintyURI) {
	public MitigationURIs(URI modelUncertaintyURI, URI mitigationUncertaintyURI) {
		this.modelUncertaintyURI = modelUncertaintyURI;
		this.mitigationUncertaintyURI = mitigationUncertaintyURI;
	}
}
