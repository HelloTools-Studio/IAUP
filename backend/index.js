const fs = require('fs');
const fastifyMultipart = require('@fastify/multipart')

// ================= Config =================
const upload_dir = 'uploads'
const base_url = 'https://iaup.liuzhen932.top/download'


const fastify = require('fastify')({
    logger: true
})
fastify.register(fastifyMultipart,
    {
        limits: {
            fieldNameSize: 640, // Max field name size in bytes
            fieldSize: 640,     // Max field value size in bytes
            fields: 5,         // Max number of non-file fields
            fileSize: 419430400,  // For multipart forms, the max file size in bytes == 50 MiB
            files: 1,           // Max number of file fields
            headerPairs: 10   // Max number of header
        }
    })

fastify.get('/', function (_request, reply) {
    reply.send({ code: 200 })
})

fastify.get('/ping', function (_request, reply) {
    reply.send({ code: 200 })
})

// ================= Core Code ================= 
fastify.post('/upload', async (request, reply) => {
    try {
        const data = await request.file()
        if (data) {
            const filename = data.filename
            fastify.log.warn(`Someone from ${request.ip} is uploading ${filename}`)
            fs.writeFile(`${upload_dir}/${filename}`, data.file, 'utf8', (err) => {
                if (err) {
                    fastify.log.error(err);
                    return reply.status(500).send({ status: 'failure', code: 500, msg: `Failed to save file(500): ${err}` })
                }
                fastify.log.warn(`Saved file ${filename}`)
            });
            return reply.status(200).send({ status: 'success', url: `${base_url}/${filename}`, code: 200 , msg: `Successfully upload file: ${filename}` })
        } else {
            fastify.log.warn(`Someone is trying the POST upload interface, but he failed!`)
            return reply.status(400).send({ status: 'failure', code: 400, msg: `Failed to upload file(400): unknown filename` })
        }
    } catch (error) {
        return reply.status(500).send({ status: 'failure', code: 500, msg: `Failed to upload file(500): ${error}` })
    }
});

fastify.get('/upload', function (_request, reply) {
    reply.status(502).send({ code: 502, msg: 'Bad Gateway' })
})

fastify.get('/download/:filename', function (request, reply) {
    const { filename } = request.params;
    fs.readFile(`${upload_dir}/${filename}`, 'utf8', (err, data) => {
        if (err) {
            return reply.status(500).send({ status: 'failure', code: 500, msg: `Failed to read file(500): ${err}` })
        }
        fastify.log.info(data)
        return reply.status(200).header('Content-Type', 'application/zip').send(data);
    });
})

// Fire the server!
fastify.listen({ port: 3000 }, function (err) {
    if (err) {
        fastify.log.error(err)
        process.exit(1)
    }
})
