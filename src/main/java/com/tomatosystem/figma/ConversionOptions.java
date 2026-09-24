package com.tomatosystem.figma;

import com.tomatosystem.figma.api.OpenApiSpec;

/**
 * Optional extras of one conversion (v2.2): an OpenAPI/Swagger document to bind DataSets/DataMaps/submissions to,
 * and the visual QA that renders the generated CLX and diffs it against Figma's own render of the frame.
 * {@link #NONE} is the plain v2.0/v2.1 conversion.
 */
public final class ConversionOptions {
	public static final ConversionOptions NONE = new ConversionOptions(null, false, "", "", FigmaApiClient.Auth.PERSONAL_TOKEN);

	/** Backend API to bind; null = no binding. */
	public final OpenApiSpec spec;
	/** Render + pixel diff after generation (needs the Figma file key and token for the reference PNG). */
	public final boolean visualQa;
	public final String fileKey;
	public final String token;
	public final FigmaApiClient.Auth auth;

	public ConversionOptions(OpenApiSpec spec, boolean visualQa, String fileKey, String token, FigmaApiClient.Auth auth) {
		this.spec = spec; this.visualQa = visualQa; this.fileKey = fileKey == null ? "" : fileKey; this.token = token == null ? "" : token;
		this.auth = auth == null ? FigmaApiClient.Auth.PERSONAL_TOKEN : auth;
	}

	/** Loads the spec when a URL/path is given ("" = none). */
	public static ConversionOptions of(String swaggerUrl, boolean visualQa, String fileKey, String token, FigmaApiClient.Auth auth) {
		OpenApiSpec spec = swaggerUrl == null || swaggerUrl.trim().isEmpty() ? null : OpenApiSpec.load(swaggerUrl.trim());
		if (spec == null && !visualQa) return NONE;
		return new ConversionOptions(spec, visualQa, fileKey, token, auth);
	}

	public ConversionOptions withSpec(OpenApiSpec newSpec) { return new ConversionOptions(newSpec, visualQa, fileKey, token, auth); }

	/** The same options with the Figma credentials the QA needs for the reference render. */
	public ConversionOptions withKey(String newFileKey, String newToken, FigmaApiClient.Auth newAuth) { return new ConversionOptions(spec, visualQa, newFileKey, newToken, newAuth); }

	public boolean isNone() { return spec == null && !visualQa; }
}
