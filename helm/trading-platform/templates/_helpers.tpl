{{/*
  Generic microservice Deployment + Service template.
  Usage: {{ include "trading.service" (dict "name" "auth-service" "port" 8081 "envVars" .envVars "Values" .Values) }}
*/}}

{{- define "trading.deployment" -}}
apiVersion: apps/v1
kind: Deployment
metadata:
  name: {{ .name }}
  labels:
    app: {{ .name }}
    version: {{ .Values.global.imageTag | quote }}
spec:
  replicas: {{ .replicas | default 1 }}
  selector:
    matchLabels:
      app: {{ .name }}
  template:
    metadata:
      labels:
        app: {{ .name }}
        version: {{ .Values.global.imageTag | quote }}
    spec:
      imagePullSecrets:
        - name: {{ .Values.global.imagePullSecret }}
      containers:
        - name: {{ .name }}
          image: {{ .Values.global.registry }}/{{ .name }}:{{ .Values.global.imageTag }}
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: {{ .port }}
              protocol: TCP
          env:
            {{- range .envVars }}
            - name: {{ .name }}
              value: {{ .value | quote }}
            {{- end }}
          resources:
            requests:
              cpu: {{ .Values.resources.requests.cpu }}
              memory: {{ .Values.resources.requests.memory }}
            limits:
              cpu: {{ .Values.resources.limits.cpu }}
              memory: {{ .Values.resources.limits.memory }}
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: {{ .port }}
            initialDelaySeconds: 30
            periodSeconds: 10
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: {{ .port }}
            initialDelaySeconds: 15
            periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: {{ .name }}
spec:
  selector:
    app: {{ .name }}
  ports:
    - port: {{ .port }}
      targetPort: {{ .port }}
      protocol: TCP
  type: ClusterIP
{{- end -}}
