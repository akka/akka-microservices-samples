SHELL_DIR := $(shell dirname $(realpath $(lastword $(MAKEFILE_LIST))))
ROOT_DIR := ${SHELL_DIR}

antora_docker_image     := local/antora-doc
antora_docker_image_tag := latest

work_dir := ${ROOT_DIR}/target

staging_dir := ${work_dir}/staging

all: build

local-preview: html-author-mode
	@echo "Access the documentation on http://localhost:8000"
	(cd target/staging/; python3 -m http.server)

show:
	echo work dir: ${work_dir}
	echo ROOT_DIR: ${ROOT_DIR}

clean:
	rm -rf ${work_dir}

docker-image:
	(cd ${ROOT_DIR}/antora-docker;  docker build -t ${antora_docker_image}:${antora_docker_image_tag} .)

# build: clean html javascaladoc_staged print-site
build: clean html print-site

html: clean  docker-image
	docker run \
		-u $(shell id -u):$(shell id -g) \
		-v ${ROOT_DIR}:/antora \
		--rm \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		docs-source/site.yml
	@echo "Done"

html-author-mode: clean docker-image
	docker run \
		-u $(shell id -u):$(shell id -g) \
		-v ${ROOT_DIR}:/antora \
		--rm \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		docs-source/author-mode-site.yml
	@echo "Done"

check-links: docker-image
	docker run \
		-v ${ROOT_DIR}:/antora \
		--rm \
		--entrypoint /bin/sh \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		-c 'find /antora/docs-source -name '*.adoc' -print0 | xargs -0 -n1 asciidoc-link-check -p -c docs-source/asciidoc-link-check-config.json'

list-todos: html docker-image
	docker run \
		-v ${ROOT_DIR}:/antora \
		--rm \
		-t ${antora_docker_image}:${antora_docker_image_tag} \
		--cache-dir=./.cache/antora \
		--entrypoint /bin/sh \
		-c 'find /antora/docs-source/build/site/cloudflow/${version} -name "*.html" -print0 | xargs -0 grep -iE "TODO|FIXME|REVIEWERS|adoc"'

${work_dir}:
	mkdir -p ${work_dir}

${staging_dir}:
	mkdir -p ${staging_dir}

print-site:
	# The result directory with the contents of this build:
	@echo "${staging_dir}"
