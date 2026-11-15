vec3 lyra_highlight(vec3 color, float threshold) {
    float luminance = dot(color, vec3(0.2126, 0.7152, 0.0722));
    float knee = max(threshold * 0.1, 0.00001);
    float transition = clamp((luminance - threshold + knee) / (2.0 * knee), 0.0, 1.0);
    float energy = max(luminance - threshold, 0.0) + transition * transition * 2.0 * knee;
    return color * energy / max(luminance, 0.00001);
}
